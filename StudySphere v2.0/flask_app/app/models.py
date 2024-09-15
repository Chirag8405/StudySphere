import mysql.connector
from mysql.connector import Error
from datetime import datetime,timedelta

# Database connection function
def create_connection():
    try:
        connection = mysql.connector.connect(
            host='localhost',
            user='Chirag8405',  # Replace with your MySQL username
            password='Chir@g_8405',  # Replace with your MySQL password
            database='StudySphere'  # Your database name
        )
        print("Connection to MySQL DB successful")
        return connection
    except Error as e:
        print(f"The error '{e}' occurred")
        return None

# User Functions
def create_user(name, email, password):
    connection = create_connection()
    if connection is None:
        return "Failed to connect to the database"
    
    try:
        with connection.cursor() as cursor:
            cursor.execute("INSERT INTO Users (name, email, password) VALUES (%s, %s, %s)",
                           (name, email, password))
            connection.commit()
            return "User created successfully!"
    except mysql.connector.IntegrityError:
        return "Email already exists!"
    except Exception as e:
        print(f"Error during user creation: {str(e)}")
        return "An error occurred during user creation."
    finally:
        connection.close()

def get_user_by_email(email):
    connection = create_connection()
    if connection is None:
        return None
    
    try:
        with connection.cursor(dictionary=True) as cursor:
            cursor.execute("SELECT * FROM Users WHERE email = %s", (email,))
            user = cursor.fetchone()
            return user
    except Exception as e:
        print(f"Error fetching user: {str(e)}")
        return None
    finally:
        connection.close()
        
        
# Assignment Functions
def create_assignment(user_id, name, due_date):
    connection = create_connection()
    if connection is None:
        return "Failed to connect to the database"
    
    try:
        with connection.cursor() as cursor:
            cursor.execute(
                "INSERT INTO Assignments (user_id, name, due_date) VALUES (%s, %s, %s)",
                (user_id, name, due_date)  # Default status set to 'pending'
            )
            connection.commit()
            return "Assignment created successfully!"
    except Exception as e:
        print(f"Error creating assignment: {str(e)}")
        return "An error occurred while creating the assignment."
    finally:
        connection.close()

def get_assignments(user_id):
    connection = create_connection()
    if connection is None:
        return None
    
    try:
        with connection.cursor(dictionary=True) as cursor:
            cursor.execute("SELECT * FROM Assignments WHERE user_id = %s", (user_id,))
            assignments = cursor.fetchall()
            return assignments
    except Exception as e:
        print(f"Error fetching assignments: {str(e)}")
        return None
    finally:
        connection.close()

def update_assignment(assignment_id, name, due_date):
     connection = create_connection()
     if connection is None:
         return "Failed to connect to the database"
    
     try:
         with connection.cursor() as cursor:
             cursor.execute(
                 "UPDATE Assignments SET name = %s, due_date = %s WHERE id = %s",
                 (name, due_date, assignment_id)
             )
             connection.commit()
             return "Assignment updated successfully!"
     except Exception as e:
         print(f"Error updating assignment: {str(e)}")
         return "An error occurred while updating the assignment."
     finally:
         connection.close()
         
def mark_assignment_as_done(assignment_id):
     connection = create_connection()
     if connection is None:
         return "Failed to connect to the database"
   
     try:
         with connection.cursor() as cursor:
             cursor.execute("UPDATE Assignments SET status = %s WHERE id = %s", ('completed', assignment_id))
             connection.commit()
             return "Assignment marked as completed successfully!"
     except Exception as e:
         print(f"Error marking assignment as completed: {str(e)}")
         return "An error occurred while marking the assignment as completed."
     finally:
         connection.close()

def mark_assignment_as_cancelled(assignment_id):
     connection = create_connection()
     if connection is None:
         return "Failed to connect to the database"
   
     try:
         with connection.cursor() as cursor:
             cursor.execute("UPDATE Assignments SET status = %s WHERE id = %s", ('cancelled', assignment_id))
             connection.commit()
             return "Assignment marked as cancelled successfully!"
     except Exception as e:
         print(f"Error marking assignment as cancelled: {str(e)}")
         return "An error occurred while marking the assignment as cancelled."
     finally:
         connection.close()

def delete_assignment(assignment_id):
    connection = create_connection()
    if connection is None:
        return "Failed to connect to the database"

    try:
        with connection.cursor() as cursor:
            cursor.execute("DELETE FROM Assignments WHERE id = %s", (assignment_id,))
            connection.commit()
            return "Assignment deleted successfully!"
    except Exception as e:
        print(f"Error deleting assignment: {str(e)}")
        return "An error occurred while deleting the assignment."
    finally:
        connection.close()
        
# Attendance Functions
def create_attendance(user_id, date, subject, status):
    connection = create_connection()
    if connection is None:
        return "Failed to connect to the database"
    
    try:
        with connection.cursor() as cursor:
            cursor.execute("INSERT INTO Attendance (user_id, date, subject, status) VALUES (%s, %s, %s, %s)",
                           (user_id, date, subject, status))
            connection.commit()
            return "Attendance recorded successfully!"
    except Exception as e:
        print(f"Error recording attendance: {str(e)}")
        return "An error occurred while recording attendance."
    finally:
        connection.close()

def get_attendance(user_id):
    connection = create_connection()
    if connection is None:
        return None
    
    try:
        with connection.cursor(dictionary=True) as cursor:
            cursor.execute("SELECT * FROM Attendance WHERE user_id = %s", (user_id,))
            attendance_records = cursor.fetchall()
            return attendance_records
    except Exception as e:
        print(f"Error fetching attendance records: {str(e)}")
        return None
    finally:
        connection.close()

# Timetable Functions
def create_timetable(user_id, subject, time, days):
    connection = create_connection()
    if connection is None:
        return "Failed to connect to the database"
    
    try:
        with connection.cursor() as cursor:
            # Convert days list to a comma-separated string
            days_str = ','.join(days)
            cursor.execute("INSERT INTO Timetable (user_id, subject, time, days) VALUES (%s, %s, %s, %s)",
                           (user_id, subject, time, days_str))
            connection.commit()
            return "Timetable entry created successfully!"
    except Exception as e:
        print(f"Error creating timetable entry: {str(e)}")
        return "An error occurred while creating the timetable entry."
    finally:
        connection.close()

def get_timetable(user_id):
    connection = create_connection()
    if connection is None:
        return None
    
    try:
        with connection.cursor(dictionary=True) as cursor:
            cursor.execute("SELECT * FROM Timetable WHERE user_id = %s", (user_id,))
            timetable_entries = cursor.fetchall()
            # Convert timedelta to a serializable format if necessary
            for entry in timetable_entries:
                if isinstance(entry['time'], timedelta):
                    entry['time'] = str(entry['time'])  # Convert to string (or use total_seconds())
            return timetable_entries
    except Exception as e:
        print(f"Error fetching timetable entries: {str(e)}")
        return None
    finally:
        connection.close()