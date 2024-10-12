import mysql.connector
from mysql.connector import Error
from datetime import datetime, timedelta

# Database connection function
def create_connection():
    try:
        connection = mysql.connector.connect(
            host='localhost',
            user='',  # Replace with your MySQL username
            password='',  # Replace with your MySQL password
            database=''  # Your database name
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
                (user_id, name, due_date)  
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
        
# Function to fetch attendance records for a user
def fetch_attendance(user_id):
    connection = create_connection()
    cursor = connection.cursor(dictionary=True)
    
    cursor.execute("SELECT a.id, t.subject, t.start_time, t.end_time, a.status, t.days FROM Attendance a JOIN Timetable t ON a.timetable_id = t.id WHERE a.user_id = %s", (user_id,))
    records = cursor.fetchall()
    
    for record in records:
        if isinstance(record['start_time'], timedelta):
            record['start_time'] = str(record['start_time'])
        if isinstance(record['end_time'], timedelta):
            record['end_time'] = str(record['end_time'])
    
    cursor.close()
    connection.close()
    return records

# Function to mark attendance for a lecture
def update_attendance(lecture_id, status, user_id, type):
    connection = create_connection()
    cursor = connection.cursor()
    
    if type == 'update':
        cursor.execute("UPDATE Attendance SET status = %s WHERE id = %s AND user_id = %s", 
                       (status, lecture_id, user_id))
    else:
        cursor.execute("INSERT INTO Attendance (user_id, timetable_id, status) VALUES (%s, %s, %s)", 
                       (user_id, lecture_id, status))
    
    connection.commit()
    cursor.close()
    connection.close()
       

# Function to add a new timetable entry
def add_timetable_entry(user_id, subject, start_time, end_time, days):
    connection = create_connection()
    cursor = connection.cursor()
    cursor.execute("INSERT INTO Timetable (user_id, subject, start_time, end_time, days) VALUES (%s, %s, %s, %s, %s)", (user_id, subject, start_time, end_time, ','.join(days)))
    connection.commit()
    cursor.close()
    connection.close()

# Function to fetch timetable records for a user
def fetch_timetable(user_id):
    connection = create_connection()
    
    if connection is None:
        return None
    try:
        with connection.cursor(dictionary=True) as cursor:
            cursor.execute("SELECT * FROM Timetable WHERE user_id = %s", (user_id,))
            records = cursor.fetchall()
            for entry in records:
                if isinstance(entry['start_time'], datetime):
                    entry['start_time'] = entry['start_time'].isoformat()
                    
                if isinstance(entry['end_time'], datetime):
                    entry['end_time'] = entry['end_time'].isoformat()  
                
                for key, value in entry.items():
                    if(isinstance(value, timedelta)):
                        entry[key] = str(value)
            return records
    except Exception as e:
        print(f"Error fetching timetable entries: {str(e)}")
        return None
    finally:
        cursor.close()
        connection.close()
        
def update_timetable_entry(entry_id, subject, start_time, end_time, days):
    connection = create_connection()
    if connection is None:
        return "Failed to connect to database"
    try:
        with connection.cursor() as cursor:
            cursor.execute("UPDATE Timetable SET subject = %s, start_time = %s, end_time = %s, days = %s WHERE id = %s", (subject, start_time, end_time, ','.join(days), entry_id)) 
            connection.commit()
            return "Timetable updated successfully!"    
    except Exception as e:
        print(f"Error updating Timetable: {str(e)}")
        return "An error occurred while updating the Timetable."
    finally:
        connection.close()
        
# Function to delete timetable entry
def delete_timetable_entry(entry_id):
    connection = create_connection()
    cursor = connection.cursor()
    
    try:
        cursor.execute("DELETE FROM Timetable WHERE id = %s", (entry_id,))
        connection.commit()
        return cursor.rowcount  
    except mysql.connector.Error as err:
        print(f"Error: {err}")
        return None
    finally:
        cursor.close()
        connection.close()

# Function to fetch user profile
def get_user_profile(user_id):
    connection = create_connection()
    if connection is None:
        return None
    
    try:
        cursor = connection.cursor(dictionary=True)
        cursor.execute("SELECT * FROM Users WHERE id = %s", (user_id,))
        user = cursor.fetchone()
        return user  
    except Exception as e:
        print(f"Error fetching user profile: {str(e)}")
        return None
    finally:
        cursor.close()
        connection.close()

# Function to fetch assignments summary
def get_assignments_summary(user_id):
    connection = create_connection()
    if connection is None:
        return None
    
    try:
        cursor = connection.cursor(dictionary=True)
        cursor.execute("SELECT COUNT(*) AS upcoming FROM assignments WHERE user_id = %s AND due_date > NOW() AND status = %s", (user_id, "pending",))
        upcoming = cursor.fetchone()['upcoming']
        
        cursor.execute("SELECT COUNT(*) AS overdue FROM assignments WHERE user_id = %s AND due_date < NOW() AND status = %s", (user_id,"pending",))
        overdue = cursor.fetchone()['overdue']
        
        return {"upcoming": upcoming, "overdue": overdue}  
    except Exception as e:
        print(f"Error fetching assignments summary: {str(e)}")
        return None
    finally:
        cursor.close()
        connection.close()

# Function to fetch attendance summary
def get_attendance_summary(user_id):
    connection = create_connection()
    if connection is None:
        return None
    
    try:
        cursor = connection.cursor(dictionary=True)
        
        cursor.execute("SELECT COUNT(*) AS attended FROM attendance WHERE user_id = %s AND status = 'present'", (user_id,))
        attended = cursor.fetchone()['attended']
        
        cursor.execute("SELECT COUNT(*) AS missed FROM attendance WHERE user_id = %s AND status != 'present'", (user_id,))
        missed = cursor.fetchone()['missed']
        
        total_classes = attended + missed
        
        percentage = (attended / total_classes * 100) if total_classes > 0 else 0
        
        return {
            "attended": attended,
            "missed": missed,
            "percentage": round(percentage, 2)  
        }
    except Exception as e:
        print(f"Error fetching attendance summary: {str(e)}")
        return None
    finally:
        cursor.close()
        connection.close()

# Function to fetch user interests
def get_user_interests(user_id):
    connection = create_connection()
    if connection is None:
        return None
    
    try:
        cursor = connection.cursor(dictionary=True)
        
        cursor.execute("SELECT interest FROM UserInterests WHERE user_id = %s", (user_id,))
        interests = [row['interest'] for row in cursor.fetchall()]  
        
        return interests  
    except Exception as e:
        print(f"Error fetching user interests: {str(e)}")
        return None
    finally:
        cursor.close()
        connection.close()
        
# Function to update user profile
def update_user_profile(user_id, name, bio):
    connection = create_connection()
    if connection is None:
        return False
    
    try:
        cursor = connection.cursor()
        cursor.execute(
            "UPDATE Users SET name = %s, bio = %s WHERE id = %s",
            (name, bio, user_id)
        )
        connection.commit()  
        return True  
    except Exception as e:
        print(f"Error updating user profile: {str(e)}")
        return False
    finally:
        cursor.close()
        connection.close()
        