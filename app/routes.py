from flask import Blueprint, jsonify, request, session, render_template, url_for, redirect
import mysql.connector
from mysql.connector import Error
from datetime import datetime, timedelta
from app.models import create_assignment, get_assignments, delete_assignment,mark_assignment_as_cancelled, mark_assignment_as_done, update_assignment, update_user_profile, delete_timetable_entry
from app.models import fetch_attendance, fetch_timetable, update_attendance, add_timetable_entry,get_user_profile, get_assignments_summary, get_attendance_summary, get_user_interests, update_timetable_entry
routes = Blueprint('routes', __name__)

# Database connection function
def create_connection():
    connection = None
    try:
        connection = mysql.connector.connect(
            host='localhost',  # Your MySQL host
            user='',  # Your MySQL username
            password='',  # Your MySQL password
            database=''  # Your database name
        )
        print("Connection to MySQL DB successful")
    except Error as e:
        print(f"The error '{e}' occurred")
    return connection

# Function to initialize the database
def init_db():
    connection = create_connection()
    if connection is None:
        print("Failed to connect to the database.")
        return  

    cursor = connection.cursor()
    try:
        with open('StudySphereSQLFILE.sql', 'r') as file:
            sql_script = file.read()
        cursor.execute(sql_script, multi=True)  
        connection.commit()
        print("Database initialized successfully.")
    except Error as e:
        print(f"The error '{e}' occurred")
    finally:
        cursor.close()
        connection.close()

init_db()

@routes.route('/')
def index():
    if 'logged_in' in session and session['logged_in']:
        return redirect(url_for('home'))  
    return redirect(url_for('login'))  

@routes.route('/home')
def index_page():
    return redirect(url_for('home'))  


def get_user_by_email(email):
    connection = create_connection()
    cursor = connection.cursor(dictionary=True)
    cursor.execute("SELECT * FROM Users WHERE email = %s", (email,))
    user = cursor.fetchone()
    cursor.close()
    connection.close()
    return user

@routes.route('/api/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        data = request.get_json()
        if not data or 'email' not in data or 'password' not in data:
            return jsonify({'message': 'Missing email or password!'}), 400
        
        email = data['email']
        password = data['password']
        
        user = get_user_by_email(email)
        if user and user['password'] == password: 
            session['user_id'] = user['id']
            session['logged_in'] = True
            return jsonify({'message': 'Login successful!', 'user_id': user['id']}), 200
        else:
            return jsonify({'message': 'Invalid email or password'}), 401

    return render_template('login.html')

@routes.route('/api/signup', methods=['POST'])
def register():
    data = request.get_json()  
    if not data or 'name' not in data or 'email' not in data or 'password' not in data:
        return jsonify({'message': 'Missing data!'}), 400
    
    try:
        connection = create_connection()
        cursor = connection.cursor()
        cursor.execute("INSERT INTO Users (name, email, password) VALUES (%s, %s, %s)", 
                       (data['name'], data['email'], data['password']))  
        connection.commit()
        return jsonify({'message': 'User registered successfully!'}), 201
    except mysql.connector.IntegrityError:
        return jsonify({'message': 'Email already exists!'}), 409
    except Exception as e:
        print(f"Error during signup: {str(e)}")
        return jsonify({'message': 'An error occurred during signup.'}), 500
    finally:
        cursor.close()
        connection.close()

# Route for the logout functionality
@routes.route('/api/logout')
def logout():
    session.pop('user_email', None)  
    session.pop('logged_in', None)    
    return redirect(url_for('login'))  

# Home route
@routes.route('/index')
def home():
    return render_template('index.html')


# Route to render assignments page
@routes.route('/assignments')
def assignments():
    return render_template('assignment.html')

# Route to render timetable page
@routes.route('/timetable')
def timetable():
    return render_template('timetable.html')

# Route to render attendance page
@routes.route('/attendance')
def attendance():
    return render_template('attendance.html')

# Route to render profile page
@routes.route('/profile')
def profile():
    return render_template('profile.html')

# Assignments API endpoints
@routes.route('/api/assignments', methods=['GET', 'POST'])
def handle_assignments():
    user_id = session.get('user_id')

    if request.method == 'GET':
        if user_id is None:
            return jsonify({'message': 'Unauthorized access!'}), 401  
        
        assignments = get_assignments(user_id)
        if assignments is None:
            return jsonify({'message': 'An error occurred while retrieving assignments.'}), 500
        
        return jsonify(assignments), 200

    if request.method == 'POST':
        data = request.get_json()
        if not data or 'name' not in data or 'due_date' not in data:
            return jsonify({'message': 'Missing data!'}), 400
        
        result = create_assignment(user_id, data['name'], data['due_date'])
        return jsonify({'message': result}), 201

# Modify Assignment
@routes.route('/api/assignments/<int:assignment_id>', methods=['PUT'])
def modify_assignment(assignment_id):
    user_id = session.get('user_id')

    if user_id is None:
         return jsonify({'message': 'Unauthorized access!'}), 401 

    data = request.get_json()
    if not data or 'name' not in data or 'due_date' not in data:
         return jsonify({'message': 'Missing data!'}), 400

    result = update_assignment(assignment_id, data['name'], data['due_date'])
    return jsonify({'message': result}), 200

# Mark Assignment as Completed
@routes.route('/api/assignments/<int:assignment_id>/complete', methods=['PUT'])
def complete_assignment(assignment_id):
     user_id = session.get('user_id')
     if user_id is None:
         return jsonify({'message': 'Unauthorized access!'}), 401 

     result = mark_assignment_as_done(assignment_id)
     return jsonify({'message': result}), 200

# Mark Assignment as Cancelled
@routes.route('/api/assignments/<int:assignment_id>/cancel', methods=['PUT'])
def cancel_assignment(assignment_id):
     user_id = session.get('user_id')

     if user_id is None:
         return jsonify({'message': 'Unauthorized access!'}), 401

     result = mark_assignment_as_cancelled(assignment_id)
     return jsonify({'message': result}), 200

# Delete Assignment
@routes.route('/api/assignments/<int:assignment_id>', methods=['DELETE'])
def remove_assignment(assignment_id):
    user_id = session.get('user_id')

    if user_id is None:
        return jsonify({'message': 'Unauthorized access!'}), 401 

    result = delete_assignment(assignment_id)
    return jsonify({'message': result}), 200

# Route to fetch attendance records for a user
@routes.route('/api/attendance', methods=['GET'])
def get_attendance():
    user_id = request.args.get('user_id')
    
    if not user_id:
        return jsonify({"message": "User ID is required"}), 400
    
    attendance_records = fetch_attendance(user_id)
    
    return jsonify(attendance_records)

# Route to mark attendance for a specific lecture
@routes.route('/api/attendance/<int:lecture_id>', methods=['PUT'])
def mark_attendance(lecture_id):
    data = request.get_json()
    
    if not data or 'status' not in data:
        return jsonify({'message': 'Invalid input'}), 400
    
    type = data['type']
    status = data['status']
    user_id = session.get('user_id')
    
    update_attendance(lecture_id, status, user_id, type)
    
    return jsonify({'message': 'Attendance updated successfully'}), 200

# Route to add a new timetable entry
@routes.route('/api/timetable', methods=['POST'])
def add_timetable():
    data = request.get_json()
    
    user_id = data.get('user_id')
    subject = data.get('subject')
    start_time = data.get('start_time')
    end_time = data.get('end_time')
    days = data.get('days')

    if not all([user_id, subject, start_time, end_time]):
        return jsonify({'message': 'All fields are required'}), 400

    add_timetable_entry(user_id, subject, start_time, end_time, days)
    
    return jsonify({'message': 'Timetable entry added successfully'}), 201

@routes.route('/api/timetable/<int:entry_id>', methods=['GET'])
def edit_timetable_entry(entry_id):
    entry = get_timetable(entry_id)
    
    if entry:
        return jsonify(entry)  
    else:
        return jsonify({"error": "Entry not found"}), 404

@routes.route('/api/timetable/<int:entry_id>', methods=['PUT'])
def update_entry(entry_id):
    data = request.get_json()
    
    subject = data.get('subject')
    start_time = data.get('start_time')
    end_time = data.get('end_time')
    days = data.get('days')  

    update_timetable_entry(entry_id, subject, start_time, end_time, days)

    return jsonify({"message": "Timetable entry updated successfully."})

# Delete Lecture
@routes.route('/api/timetable/<int:entry_id>', methods=['DELETE'])
def delete_timetable_entry_route(entry_id):
    result = delete_timetable_entry(entry_id)
    
    if result is None:
        return jsonify({'message': 'Error deleting entry.'}), 500
    elif result == 0:
        return jsonify({'message': 'Entry not found.'}), 404
    
    return jsonify({'message': 'Timetable entry deleted successfully.'}), 200

# Route to fetch timetable records for a user
@routes.route('/api/timetable', methods=['GET'])
def get_timetable():
    user_id = session.get('user_id')
    
    if not user_id:
        return jsonify({"message": "User ID is required"}), 400
    
    timetable_entries = fetch_timetable(user_id)
    return jsonify(timetable_entries)

# Route to fetch timetable records for Attendance page
@routes.route('/api/attendance_timetable', methods=['GET'])
def get_attendance_timetable():
    user_id = session.get('user_id')
    today = datetime.now().strftime('%A')  
    try:
        connection = create_connection()
        cursor = connection.cursor(dictionary=True)
        cursor.execute("SELECT * FROM Timetable WHERE user_id = %s and FIND_IN_SET(%s, days)", (user_id, today,))
        entries = cursor.fetchall() 
        
        for entry in entries:
            if isinstance(entry['start_time'], timedelta):  
                entry['start_time'] = str(entry['start_time'])  
                
            if isinstance(entry['end_time'], timedelta):  
                entry['end_time'] = str(entry['end_time'])
                
            for key, value in entry.items():
                if(isinstance(value, timedelta)):
                    entry[key] = str(value)
        return  jsonify(entries)  
    except Exception as e:
        print(f"Error fetching Attendance Records: {str(e)}")
        return jsonify({'message': 'An error occurred while fetching Attendance Records.'}), 500
    finally:
        cursor.close()
        connection.close()                 

    
@routes.route('/api/attendance/extra', methods=['POST'])
def add_extra_attendance():
    data = request.get_json()
    user_id = data.get('user_id')
    subject = data.get('subject')
    start_time = data.get('start_time')
    end_time = data.get('end_time')
    days = data.get('days')

# Validate input
    if not all([user_id, subject, start_time, end_time, days]):
        return jsonify({"message": "All fields are required"}), 400

    return jsonify({"message": "Extra lecture added successfully"}), 201
    
# Route to fetch user profile
@routes.route('/api/profile', methods=['GET'])
def fetch_user_profile():
    user_id = request.args.get('user_id')
    
    if not user_id:
        return jsonify({"message": "User ID is required"}), 400
    
    profile = get_user_profile(user_id)
    
    if profile is None:
        return jsonify({"message": "User not found."}), 404
    
    return jsonify(profile), 200

# Route to fetch assignments summary
@routes.route('/api/assignments/summary', methods=['GET'])
def fetch_assignments_summary():
    user_id = request.args.get('user_id')
    
    if not user_id:
        return jsonify({"message": "User ID is required"}), 400
    
    summary = get_assignments_summary(user_id)
    
    if summary is None:
        return jsonify({"message": "An error occurred while fetching assignments summary."}), 500
    
    return jsonify(summary), 200

# Route to fetch attendance summary
@routes.route('/api/attendance/summary', methods=['GET'])
def fetch_attendance_summary():
    user_id = request.args.get('user_id')
    
    if not user_id:
        return jsonify({"message": "User ID is required"}), 400
    
    summary = get_attendance_summary(user_id)
    
    if summary is None:
        return jsonify({"message": "An error occurred while fetching attendance summary."}), 500
    
    return jsonify(summary), 200

# Route to fetch user interests
@routes.route('/api/interests', methods=['GET'])
def fetch_user_interests():
    user_id = request.args.get('user_id')
    
    if not user_id:
        return jsonify({"message": "User ID is required"}), 400
    
    interests = get_user_interests(user_id)
    
    if interests is None:
        return jsonify({"message": "An error occurred while fetching interests."}), 500
    
    return jsonify(interests), 200

# Route to update user profile
@routes.route('/api/profile/update', methods=['POST'])
def update_profile():
    data = request.get_json()
    
    user_id = data.get('user_id')
    name = data.get('name')
    bio = data.get('bio')

    # Validate input
    if not all([user_id, name, bio]):
        return jsonify({"message": "All fields are required"}), 400

    result = update_user_profile(user_id, name, bio)

    if result:
        return jsonify({"message": "Profile updated successfully!"}), 200
    else:
        return jsonify({"message": "Failed to update profile."}), 500

# Error handling for 404
@routes.errorhandler(404)
def not_found(error):
    return jsonify({'message': 'Resource not found!'}), 404