// script.js

// Function to get the user ID from the session or URL parameters
  
  // Function to fetch assignments for the user
  function fetchAssignments(userId) {
    fetch(`/api/assignments?user_id=${userId}`)
      .then(response => response.json())
      .then(data => {
        // Display the assignments in the UI
        displayAssignments(data);
      })
      .catch(error => {
        console.error('Error fetching assignments:', error);
      });
  }
  
  // Function to create an assignment
  function createAssignment(userId, name, dueDate) {
    fetch('/api/assignments', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ user_id: userId, name, due_date: dueDate })
    })
      .then(response => response.json())
      .then(data => {
        // Clear the form and refresh the assignments
        document.getElementById('assignment-name').value = '';
        document.getElementById('due-date').value = '';
        fetchAssignments(userId);
      })
      .catch(error => {
        console.error('Error creating assignment:', error);
      });
  }
  
  // Function to fetch attendance records for the user
  function fetchAttendance(userId) {
    fetch(`/api/attendance?user_id=${userId}`)
      .then(response => response.json())
      .then(data => {
        // Display the attendance records in the UI
        displayAttendance(data);
      })
      .catch(error => {
        console.error('Error fetching attendance:', error);
      });
  }
  
  // Function to record attendance
  function recordAttendance(userId, subject, status) {
    fetch('/api/attendance', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ user_id: userId, subject, status })
    })
      .then(response => response.json())
      .then(data => {
        // Clear the form and refresh the attendance records
        document.getElementById('subject').value = '';
        document.getElementById('status').value = '';
        fetchAttendance(userId);
      })
      .catch(error => {
        console.error('Error recording attendance:', error);
      });
  }
  
  // Function to fetch user profile information
  function fetchUserProfile(userId) {
    fetch(`/api/users/${userId}`)
      .then(response => response.json())
      .then(data => {
        // Display the user profile information in the UI
        displayUserProfile(data);
      })
      .catch(error => {
        console.error('Error fetching user profile:', error);
      });
  }
  
  // Function to update the user profile
  function updateProfile(userId, name, email, bio) {
    fetch(`/api/users/${userId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ name, email, bio })
    })
      .then(response => response.json())
      .then(data => {
        // Display a success message or refresh the profile
        displaySuccessMessage('Profile updated successfully!');
      })
      .catch(error => {
        console.error('Error updating profile:', error);
      });
  }
  
  // Function to display assignments in the UI
  function displayAssignments(assignments) {
    const assignmentsContainer = document.getElementById('assignments-container');
    assignmentsContainer.innerHTML = '';
  
    assignments.forEach(assignment => {
      const assignmentElement = document.createElement('div');
      assignmentElement.classList.add('assignment');
  
      const nameElement = document.createElement('h3');
      nameElement.textContent = assignment.name;
  
      const dueDateElement = document.createElement('p');
      dueDateElement.textContent = `Due Date: ${assignment.due_date}`;
  
      const completedElement = document.createElement('p');
      completedElement.textContent = `Completed: ${assignment.completed ? 'Yes' : 'No'}`;
  
      assignmentElement.appendChild(nameElement);
      assignmentElement.appendChild(dueDateElement);
      assignmentElement.appendChild(completedElement);
  
      assignmentsContainer.appendChild(assignmentElement);
    });
  }
  
  // Function to display attendance records in the UI
  function displayAttendance(attendance) {
    const attendanceContainer = document.getElementById('attendance-container');
    attendanceContainer.innerHTML = '';
  
    attendance.forEach(record => {
      const recordElement = document.createElement('div');
      recordElement.classList.add('attendance-record');
  
      const dateElement = document.createElement('p');
      dateElement.textContent = `Date: ${record.date}`;
  
      const subjectElement = document.createElement('p');
      subjectElement.textContent = `Subject: ${record.subject}`;
  
      const statusElement = document.createElement('p');
      statusElement.textContent = `Status: ${record.status}`;
  
      recordElement.appendChild(dateElement);
      recordElement.appendChild(subjectElement);
      recordElement.appendChild(statusElement);
  
      attendanceContainer.appendChild(recordElement);
    });
  }
  
  // Function to display user profile information in the UI
  function displayUserProfile(userProfile) {
    const nameElement = document.getElementById('name');
    const emailElement = document.getElementById('email');
    const bioElement = document.getElementById('bio');
  
    nameElement.value = userProfile.name;
    emailElement.value = userProfile.email;
    bioElement.value = userProfile.bio;
  }
  
  // Function to display a success message to the user
  function displaySuccessMessage(message) {
    alert(message);
  }

// Common utility functions

function getUserId() {
  return sessionStorage.getItem('userId'); // Retrieve user ID from session storage
}