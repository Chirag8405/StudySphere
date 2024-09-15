from flask import Flask, render_template, request, jsonify, redirect, url_for, session
import mysql.connector
from mysql.connector import Error
from datetime import datetime
from app.routes import routes

app = Flask(__name__)
app.secret_key = '12345'  # Set a secret key for session management

# Register the routes blueprint with the /api prefix
app.register_blueprint(routes, url_prefix='/api')

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

@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        email = request.form['email']
        password = request.form['password']
        
        user = get_user_by_email(email)
        if user and user['password'] == password:  # Compare passwords directly
            session['user_id'] = user['id']
            session['logged_in'] = True
            return jsonify({'message': 'Login Successful!', 'user_id': user[id]}), 200 
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

# Route to render assignments page
@app.route('/assignment')
def assignments():
    return render_template('assignment.html')

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