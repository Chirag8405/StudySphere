from flask import Blueprint, jsonify, request, session, render_template, url_for, redirect
import mysql.connector
from mysql.connector import Error
from datetime import datetime
from app.models import create_assignment, get_assignments, delete_assignment, update_assignment, mark_assignment_as_done, mark_assignment_as_cancelled
from app.models import create_attendance, get_attendance, create_timetable, get_timetable
routes = Blueprint('routes', __name__)

# Database connection function
def create_connection():
    connection = None
    try:
        connection = mysql.connector.connect(
            host='localhost',  # Your MySQL host
            user='root',  # Your MySQL username
            password='Chir@g_8405',  # Your MySQL password
            database='StudySphere'  # Your database name
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
        return  # Exit the function if connection fails

    cursor = connection.cursor()
    try:
        with open('StudySphereSQLFILE.sql', 'r') as file:
            sql_script = file.read()
        cursor.execute(sql_script, multi=True)  # Execute multiple statements
        connection.commit()
        print("Database initialized successfully.")
    except Error as e:
        print(f"The error '{e}' occurred")
    finally:
        cursor.close()
        connection.close()

# Call init_db to create tables if they don't exist
init_db()

@routes.route('/')
def index():
    if 'logged_in' in session and session['logged_in']:
        return redirect(url_for('home'))  # Redirect to the home route
    return redirect(url_for('login'))  # Redirect to the login page

@routes.route('/home')
def index_page():
    return redirect(url_for('home'))  #Redirect to home route


def get_user_by_email(email):
    connection = create_connection()
    cursor = connection.cursor(dictionary=True)
    cursor.execute("SELECT * FROM Users WHERE email = %s", (email,))
    user = cursor.fetchone()
    cursor.close()
    connection.close()
    return user

@routes.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        # Use request.get_json() to get JSON data
        data = request.get_json()
        if not data or 'email' not in data or 'password' not in data:
            return jsonify({'message': 'Missing email or password!'}), 400
        
        email = data['email']
        password = data['password']
        
        user = get_user_by_email(email)
        if user and user['password'] == password:  # Compare passwords directly
            session['user_id'] = user['id']
            session['logged_in'] = True
            return jsonify({'message': 'Login successful!', 'user_id': user['id']}), 200
        else:
            return jsonify({'message': 'Invalid email or password'}), 401

    return render_template('login.html')

@routes.route('/signup', methods=['POST'])
def register():
    data = request.get_json()  # Expecting JSON input
    if not data or 'name' not in data or 'email' not in data or 'password' not in data:
        return jsonify({'message': 'Missing data!'}), 400
    
    try:
        connection = create_connection()
        cursor = connection.cursor()
        cursor.execute("INSERT INTO Users (name, email, password) VALUES (%s, %s, %s)", 
                       (data['name'], data['email'], data['password']))  # Store password as is
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
@routes.route('/logout')
def logout():
    session.pop('user_email', None)  # Remove user email from session
    session.pop('logged_in', None)    # Remove logged-in status
    return redirect(url_for('home'))  # Redirect to the index page

# Home route
@routes.route('/index')
def home():
    return render_template('index.html')


# Route to render assignments page
@routes.route('/assignments')
def assignments():
    return render_template('assignment.html')

# Route to render attendance page
@routes.route('/attendance')
def attendance():
    return render_template('attendance.html')

# Route to render profile page
@routes.route('/profile')
def profile():
    return render_template('profile.html')

# Assignments API endpoints
@routes.route('/assignments', methods=['GET', 'POST'])
def handle_assignments():
    user_id = session.get('user_id')

    if request.method == 'GET':
        if user_id is None:
            return jsonify({'message': 'Unauthorized access!'}), 401  # User must be logged in
        
        result_assignments = get_assignments(user_id)
        if result_assignments is None:
            return jsonify({'message': 'An error occurred while retrieving assignments.'}), 500
        
        return jsonify(result_assignments), 200

    if request.method == 'POST':
        data = request.get_json()
        if not data or 'name' not in data or 'due_date' not in data:
            return jsonify({'message': 'Missing data!'}), 400
        
        result = create_assignment(user_id, data['name'], data['due_date'])
        return jsonify({'message': result}), 201

# Modify Assignment
@routes.route('/assignments/<int:assignment_id>', methods=['PUT'])
def modify_assignment(assignment_id):
    user_id = session.get('user_id')

    if user_id is None:
        return jsonify({'message': 'Unauthorized access!'}), 401  # User must be logged in

    data = request.get_json()
    if not data or 'name' not in data or 'due_date' not in data:
        return jsonify({'message': 'Missing data!'}), 400

    result = update_assignment(assignment_id, data['name'], data['due_date'])
    return jsonify({'message': result}), 200

# Mark Assignment as Completed
@routes.route('/assignments/<int:assignment_id>/complete', methods=['PUT'])
def complete_assignment(assignment_id):
    user_id = session.get('user_id')

    if user_id is None:
        return jsonify({'message': 'Unauthorized access!'}), 401  # User must be logged in

    result = mark_assignment_as_done(assignment_id)
    return jsonify({'message': result}), 200

# Mark Assignment as Cancelled
@routes.route('/assignments/<int:assignment_id>/cancel', methods=['PUT'])
def cancel_assignment(assignment_id):
    user_id = session.get('user_id')

    if user_id is None:
        return jsonify({'message': 'Unauthorized access!'}), 401  # User must be logged in

    result = mark_assignment_as_cancelled(assignment_id)
    return jsonify({'message': result}), 200

# Delete Assignment
@routes.route('/assignments/<int:assignment_id>', methods=['DELETE'])
def remove_assignment(assignment_id):
    user_id = session.get('user_id')

    if user_id is None:
        return jsonify({'message': 'Unauthorized access!'}), 401  # User must be logged in

    result = delete_assignment(assignment_id)
    return jsonify({'message': result}), 200

# Attendance API endpoints
@routes.route('/attendance', methods=['GET', 'POST'])
def handle_attendance():
    user_id = session.get('user_id')
    
    if request.method == 'GET':
        if user_id is None:
            return jsonify({'message': 'Unauthorized access!'}), 401  # User must be logged in

        attendance_records = get_attendance(user_id) or []  # Return empty list if None
        return jsonify(attendance_records), 200
    
    if request.method == 'POST':
        data = request.get_json()
        if not data or 'subject' not in data or 'status' not in data:
            return jsonify({'message': 'Missing data!'}), 400
        
        # Record attendance
        result = create_attendance(user_id, datetime.now().date(), data['subject'], data['status'])
        return jsonify({'message': result}), 201

# Timetable API endpoints
@routes.route('/timetable', methods=['GET', 'POST'])
def handle_timetable():
    user_id = session.get('user_id')
    
    if request.method == 'GET':
        if user_id is None:
            return jsonify({'message': 'Unauthorized access!'}), 401  # User must be logged in

        timetable_entries = get_timetable(user_id) or []  # Return empty list if None
        return jsonify(timetable_entries), 200
    
    if request.method == 'POST':
        data = request.get_json()
        if not data or 'subject' not in data or 'time' not in data or 'days' not in data:
            return jsonify({'message': 'Missing data!'}), 400
        
        # Create timetable entry
        result = create_timetable(user_id, data['subject'], data['time'], data['days'])
        return jsonify({'message': result}), 201
    
    
# Error handling for 404
@routes.errorhandler(404)
def not_found(error):
    return jsonify({'message': 'Resource not found!'}), 404