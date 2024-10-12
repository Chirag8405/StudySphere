from flask import Flask
import mysql.connector
from mysql.connector import Error

def create_app():
    app = Flask(__name__)
    app.secret_key=''  #your secret key
    # Database connection function
    def create_connection():
        connection = None
        try:
            connection = mysql.connector.connect(
                host='localhost',
                user='',  # Replace with your MySQL username
                password='',  # Replace with your MySQL password
                database=''  # Your database name
            )
            print("Connection to MySQL DB successful")
        except Error as e:
            print(f"The error '{e}' occurred")
        return connection

    # Register routes
    from app.routes import routes
    app.register_blueprint(routes)

    # Set up database connection
    app.config['DB_CONNECTION'] = create_connection()

    return app