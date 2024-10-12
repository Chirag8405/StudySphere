from flask import Flask, render_template, request, jsonify, redirect, url_for, session, send_file
import mysql.connector
from mysql.connector import Error
from app.routes import routes

app = Flask(__name__)
app.secret_key = ''  # Set a secret key for session management

# Register the routes blueprint with the /api prefix
app.register_blueprint(routes)

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

@app.route('/')
def index():
    if 'logged_in' in session and session['logged_in']:
        return redirect(url_for('home'))  # Redirect to the home route
    return redirect(url_for('routes.login'))  # Redirect to the login page

def get_user_by_email(email):
    connection = create_connection()
    cursor = connection.cursor(dictionary=True)
    cursor.execute("SELECT * FROM Users WHERE email = %s", (email,))
    user = cursor.fetchone()
    cursor.close()
    connection.close()
    return user

@app.route('/api/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        data = request.get_json() #Get JSON data from request
        email = data.get('email')
        password = data.get('password')
        
        user = get_user_by_email(email)
        if user and user['password'] == password:  # Compare passwords directly
            session['user_id'] = user['id']
            session['logged_in'] = True
            return jsonify({'message': 'Login Successful!', 'user_id': user['id']}), 200 
        else:
            return jsonify({'message': 'Invalid email or password'}), 401

    return render_template('login.html')

# Route for the logout functionality
@app.route('/logout')
def logout():
    session.pop('user_id', None)  # Remove user email from session
    session.pop('logged_in', None)    # Remove logged-in status
    return redirect(url_for('login'))  # Redirect to the index page

# Home route
@app.route('/index')
def home():
    return render_template('index.html')

@app.route('/manifest.json')
def serve_manifest():
    return send_file('manifest.json', mimetype='application/manifest+json')

@app.route('/sw.js')
def serve_sw():
    return send_file('service-worker.js', mimetype='application/javascript')


# Route to render assignments page
@app.route('/assignment')
def assignments():
    return render_template('assignment.html')

# Route to handle assignments API
@app.route('/api/assignments', methods=['GET', 'POST'])
def handle_assignments():
    if request.method == 'GET':
        user_id = session.get('user_id')
        if user_id is None:
            return jsonify({'message': 'Unauthorized access!'}), 401  # User must be logged in

        assignments = get_assignments(user_id)
        if assignments is None:
            return jsonify({'message': 'An error occurred while retrieving assignments.'}), 500

        return jsonify(assignments), 200  # Return assignments as JSON

    if request.method == 'POST':
        data = request.get_json()
        if not data or 'name' not in data or 'due_date' not in data:
            return jsonify({'message': 'Missing data!'}), 400
        
        user_id = session.get('user_id')
        result = create_assignment(user_id, data['name'], data['due_date'])
        return jsonify({'message': result}), 201
    
# Function to get assignments for a specific user
def get_assignments(user_id):
    connection = create_connection()
    if connection is None:
        return None
    
    try:
        with connection.cursor(dictionary=True) as cursor:
            cursor.execute("SELECT * FROM assignments WHERE user_id = %s", (user_id,))
            assignments = cursor.fetchall()
            return jsonify(assignments), 200
    except Exception as e:
        print(f"Error fetching assignments: {str(e)}")
        return None
    finally:
        connection.close()

# Function to create an assignment
def create_assignment(user_id, name, due_date):
    connection = create_connection()
    if connection is None:
        return "Failed to connect to the database"
    
    try:
        with connection.cursor() as cursor:
            cursor.execute(
                "INSERT INTO assignments (user_id, name, due_date) VALUES (%s, %s, %s)",
                (user_id, name, due_date)
            )
            connection.commit()
            return "Assignment created successfully!"
    except Exception as e:
        print(f"Error creating assignment: {str(e)}")
        return "An error occurred while creating the assignment."
    finally:
        connection.close()
        
# Route to delete an assignment
@app.route('/api/assignments/<int:assignment_id>', methods=['DELETE'])
def delete_assignment(assignment_id):
    user_id = session.get('user_id')
    connection = create_connection()
    if connection is None:
        return jsonify({'message': 'Failed to connect to the database'}), 500

    try:
        with connection.cursor() as cursor:
            cursor.execute("DELETE FROM assignments WHERE id = %s AND user_id = %s", (assignment_id, user_id))
            connection.commit()
            return jsonify({'message': 'Assignment deleted successfully!'}), 200
    except Exception as e:
        print(f"Error deleting assignment: {str(e)}")
        return jsonify({'message': 'An error occurred while deleting the assignment.'}), 500
    finally:
        connection.close()
        
# Route to render timetable page
@app.route('/timetable')
def timetable():
    return render_template('timetable.html')

# Route to render attendance page
@app.route('/attendance')
def attendance():
    return render_template('attendance.html')

# Route to render profile page
@app.route('/profile')
def profile():
    return render_template('profile.html')

# Run the application
if __name__ == '__main__':
    app.run(debug=True)