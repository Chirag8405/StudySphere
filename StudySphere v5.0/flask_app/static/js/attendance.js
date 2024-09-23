document.addEventListener("DOMContentLoaded", function () {
    const userId = getUserId(); // Retrieve user ID from session storage

    if (!userId) {
        console.error("User ID not found in session storage. User may not be logged in.");
        return; // Exit if user ID is not found
    }

    fetchAttendance(userId); // Fetch attendance records on page load
    fetchTimetable(); // Fetch timetable data on page load

    const createExtraLectureBtn = document.getElementById("create-extra-lecture-btn");
    const createExtraLectureSection = document.getElementById("create-extra-lecture");
    const extraLectureForm = document.getElementById("extra-lecture-form");

    // Toggle visibility of the extra lecture creation section
    createExtraLectureBtn.addEventListener("click", () => {
        createExtraLectureSection.style.display =
            createExtraLectureSection.style.display === "none" ? "block" : "none";
    });

    // Handle form submission for creating a new extra lecture entry
    extraLectureForm.addEventListener("submit", function (event) {
        event.preventDefault(); // Prevent default form submission
        const subject = document.getElementById("extra-subject").value;
        const startTime = document.getElementById("extra-start-time").value; // Get start time
        const endTime = document.getElementById("extra-end-time").value; // Get end time
        const days = Array.from(document.getElementById("extra-days").selectedOptions).map((option) => option.value);
        recordExtraLecture(userId, subject, startTime, endTime, days);
    });

    // Function to retrieve user ID from session storage
    function getUserId() {
        return sessionStorage.getItem("userId"); // Retrieve user ID from session storage
    }

    // Function to fetch attendance records
    function fetchAttendance(userId) {
        fetch(`/api/attendance?user_id=${userId}`)
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Network response was not ok");
                }
                return response.json();
            })
            .then((data) => {
                displayAttendance(data);
            })
            .catch((error) => {
                console.error("Error fetching attendance:", error);
                alert('Could not fetch attendance records. Please try again later.');
            });
    }

// Function to display attendance records
function displayAttendance(attendanceRecords) {
    const attendanceList = document.getElementById("attendanceList");
    attendanceList.innerHTML = ""; // Clear existing entries
  
    if (!attendanceRecords || attendanceRecords.length === 0) {
      const noRecordsMessage = document.createElement("p");
      noRecordsMessage.textContent = "No attendance records available.";
      attendanceList.appendChild(noRecordsMessage);
      return;
    }
  
    // Get the current day
    const currentDay = new Date().toLocaleDateString('en-US', { weekday: 'long' });
  
    attendanceRecords.forEach((entry) => {
      // Check if the entry's days array includes the current day
      if (entry.days.includes(currentDay)) {
        const entryElement = document.createElement("div");
        entryElement.classList.add("attendance-entry");
  
        const subjectElement = document.createElement("p");
        subjectElement.textContent = `Subject: ${entry.subject}`;
  
        const timeElement = document.createElement("p");
        timeElement.textContent = `Time: ${entry.start_time} - ${entry.end_time}`;
  
        const daysElement = document.createElement("p");
        daysElement.textContent = `Days: ${entry.days.join(", ")}`;
  
        const statusElement = document.createElement("p");
        statusElement.textContent = `Status: ${entry.status}`;
  
        const percentageElement = document.createElement("span");
        percentageElement.classList.add("attendance-percentage");
        percentageElement.textContent = `Attendance %: ${calculateIndividualAttendance(entry)}%`;
  
        // Create buttons for marking attendance
        const markPresentBtn = document.createElement("button");
        markPresentBtn.textContent = "Mark Present";
        markPresentBtn.onclick = () => markAttendance(entry.id, "present");
  
        const markAbsentBtn = document.createElement("button");
        markAbsentBtn.textContent = "Mark Absent";
        markAbsentBtn.onclick = () => markAttendance(entry.id, "absent");
  
        const markCancelledBtn = document.createElement("button");
        markCancelledBtn.textContent = "Mark Cancelled";
        markCancelledBtn.onclick = () => markAttendance(entry.id, "cancelled");
  
        entryElement.appendChild(subjectElement);
        entryElement.appendChild(timeElement);
        entryElement.appendChild(daysElement);
        entryElement.appendChild(statusElement);
        entryElement.appendChild(percentageElement);
        entryElement.appendChild(markPresentBtn);
        entryElement.appendChild(markAbsentBtn);
        entryElement.appendChild(markCancelledBtn);
  
        attendanceList.appendChild(entryElement);
      }
    });
  }

    // Function to calculate individual lecture attendance percentage
    function calculateIndividualAttendance(entry) {
        const totalLectures = entry.totalLectures || 1; 
        const attendedLectures = (entry.status === "present") ? 1 : 0; 
        return ((attendedLectures / totalLectures) * 100).toFixed(2); 
    }

    // Function to mark attendance for a lecture
    function markAttendance(lectureId, status) {
        fetch(`/api/attendance/${lectureId}`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ status: status }),
        })
        .then((response) => {
            if (!response.ok) {
                throw new Error("Network response was not ok");
            }
            return response.json();
        })
        .then((data) => {
            console.log(data.message);
            fetchAttendance(getUserId()); 
            alert('Attendance marked successfully.');
        })
        .catch((error) => {
            console.error("Error marking attendance:", error);
            alert('Could not mark attendance. Please try again later.');
        });
    }

    // Function to record a new extra lecture
    function recordExtraLecture(userId, subject, startTime, endTime, days) {
        fetch('/api/attendance/extra', { 
          method: "POST",
          headers: {
              "Content-Type": "application/json",
          },
          body: JSON.stringify({ user_id: userId, subject: subject, start_time: startTime, end_time: endTime, days: days }),
      })
      .then((response) => {
          if (!response.ok) {
              throw new Error("Network response was not ok");
          }
          return response.json();
      })
      .then((data) => {
          console.log(data.message);
          fetchAttendance(userId); 
          document.getElementById("extra-lecture-form").reset(); 
          document.getElementById("create-extra-lecture").style.display = "none"; 
          alert('Extra lecture added successfully.');
      })
      .catch((error) => {
          console.error("Error recording extra lecture:", error);
          alert('Could not add extra lecture. Please try again later.');
      });
    }

    // Function to fetch timetable data
    function fetchTimetable() {
        fetch('/api/timetable') 
          .then((response) => {
              if (!response.ok) {
                  throw new Error('Network response was not ok');
              }
              return response.json();
          })
          .then((data) => {
              displayTimetable(data);
          })
          .catch((error) => {
              console.error('Error fetching timetable:', error);
              alert('Could not fetch timetable. Please try again later.');
          });
    }

    // Function to display timetable data
    function displayTimetable(timetableData) {
        const timetableList = document.getElementById('timetableList');
        timetableList.innerHTML = ''; 

        timetableData.forEach((entry) => {
          const entryElement = document.createElement('div');
          entryElement.classList.add('timetable-entry');

          const subjectElement = document.createElement('p');
          subjectElement.textContent = `Subject: ${entry.subject}`;

          const timeElement = document.createElement('p');
          timeElement.textContent = `Time: ${entry.start_time} - ${entry.end_time}`;

          const daysElement = document.createElement('p');
          
          // Check if days is an array before using join
          if (Array.isArray(entry.days)) {
              daysElement.textContent = `Days: ${entry.days.join(', ')}`;
          } else {
              daysElement.textContent = `Days: ${entry.days}`; 
          }

          entryElement.appendChild(subjectElement);
          entryElement.appendChild(timeElement);
          entryElement.appendChild(daysElement);
          
          timetableList.appendChild(entryElement);
      });
  }
});