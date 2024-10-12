document.addEventListener("DOMContentLoaded", function () {
  const userId = getUserId(); 

  if (!userId) {
    console.error(
      "User ID not found in session storage. User may not be logged in."
    );
    return; 
  }

  // Fetch attendance and timetable data on page load
  fetchAttendance(userId);
  fetchTimetable(userId);

  const createExtraLectureBtn = document.getElementById(
    "create-extra-lecture-btn"
  );
  const createExtraLectureSection = document.getElementById(
    "create-extra-lecture"
  );
  const extraLectureForm = document.getElementById("extra-lecture-form");

  createExtraLectureBtn.addEventListener("click", () => {
    createExtraLectureSection.style.display =
      createExtraLectureSection.style.display === "none" ? "block" : "none";
  });

  extraLectureForm.addEventListener("submit", function (event) {
    event.preventDefault(); 
    const subject = document.getElementById("extra-subject").value;
    const startTime = document.getElementById("extra-start-time").value;
    const endTime = document.getElementById("extra-end-time").value; 
    const day = document.getElementById("extra-days").value;
    recordExtraLecture(userId, subject, startTime, endTime, day);
  });

  // Function to retrieve user ID from session storage
  function getUserId() {
    return sessionStorage.getItem("userId");
  }

  // Fetch attendance records
  function fetchAttendance(userId) {
    fetch(`/api/attendance?user_id=${userId}`)
      .then((response) => {
        if (!response.ok) throw new Error("Network response was not ok");
        return response.json();
      })
      .then((data) => {
        console.log("Attendance data:", data);
        displayAttendance(data);
      })
      .catch((error) => {
        console.error("Error fetching attendance:", error);
        alert("Could not fetch attendance records. Please try again later.");
      });
  }

  // Display attendance records for today only
  function displayAttendance(attendanceRecords) {
    const attendanceList = document.getElementById("attendanceList");
    attendanceList.innerHTML = ""; 

    if (!attendanceRecords || attendanceRecords.length === 0) {
      const noRecordsMessage = document.createElement("p");
      noRecordsMessage.textContent = "No attendance records available.";
      attendanceList.appendChild(noRecordsMessage);
      return;
    }

    // Get the current day
    const currentDay = new Date().toLocaleDateString("en-US", {
      weekday: "long",
    });

    attendanceRecords.forEach((entry) => {
      console.log(
        `Checking entry day ${entry.days} against current day ${currentDay}`
      );

      // Check if the entry's day matches the current day
      if (entry.days === currentDay) {
        const entryElement = document.createElement("div");
        entryElement.classList.add("timetable-entry");

        document.getElementById(entry.subject);

        const subjectElement = document.createElement("p");
        subjectElement.textContent = `${entry.subject}`;

        let new_start_time = entry.start_time.split(":").slice(0, 2).join(":");
        let new_end_time = entry.end_time.split(":").slice(0, 2).join(":");
        const timeElement = document.createElement("p");
        timeElement.textContent = `Time: ${new_start_time} - ${new_end_time}`;

        // Calculate attendance for this entry
        const attendanceRecords =
          JSON.parse(localStorage.getItem("attendanceRecords")) || [];

        const attendanceP = document.createElement("p");
        attendanceP.textContent = `Attendance: ${Math.ceil(
          calculateIndividualAttendance(entry)
        )}%`;

        const attendanceBtnDiv = document.createElement("div");

        const markPresentBtn = document.createElement("button");
        markPresentBtn.textContent = "Mark Present";
        markPresentBtn.onclick = () =>
          markAttendance(entry.id, "present", "update", entry.subject);

        const markAbsentBtn = document.createElement("button");
        markAbsentBtn.textContent = "Mark Absent";
        markAbsentBtn.onclick = () =>
          markAttendance(entry.id, "absent", "update", entry.subject);

        const markCancelledBtn = document.createElement("button");
        markCancelledBtn.textContent = "Mark Cancelled";
        markCancelledBtn.onclick = () =>
          markAttendance(entry.id, "cancelled", "update", entry.subject);

        entryElement.appendChild(subjectElement);
        entryElement.appendChild(timeElement);
        entryElement.appendChild(attendanceP);
        attendanceBtnDiv.appendChild(markPresentBtn);
        attendanceBtnDiv.appendChild(markAbsentBtn);
        attendanceBtnDiv.appendChild(markCancelledBtn);
        entryElement.appendChild(attendanceBtnDiv);

        attendanceList.appendChild(entryElement);
        // }
      }
    });
  }

  // Calculate individual lecture attendance percentage
  function calculateIndividualAttendance(entry) {
    const totalLectures = entry.totalLectures || 1;
    const attendedLectures = entry.status === "present" ? 1 : 0;
    return ((attendedLectures / totalLectures) * 100).toFixed(2);
  }

  // Mark attendance for a lecture
  function markAttendance(lectureId, status, type, subject) {
    fetch(`/api/attendance/${lectureId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status: status, type: type }),
    })
      .then((response) => {
        if (!response.ok) throw new Error("Network response was not ok");
        return response.json();
      })
      .then((data) => {
        console.log("Marked attendance:", data);

        // Fetch updated attendance and timetable data
        fetchAttendance(getUserId());
        alert("Attendance marked successfully.");
        if (status === "absent" || status === "cancelled")
          document.getElementById(subject).style.display = "block";
      })
      .catch((error) => {
        console.error("Error marking attendance:", error);
        alert("Could not mark attendance. Please try again later.");
      });
  }

  // Record a new extra lecture
  function recordExtraLecture(userId, subject, startTime, endTime, day) {
    fetch("/api/attendance/extra", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        user_id: userId,
        subject: subject,
        start_time: startTime,
        end_time: endTime,
        day: day,
      }),
    })
      .then((response) => {
        if (!response.ok) throw new Error("Network response was not ok");
        return response.json();
      })
      .then((data) => {
        console.log("Extra lecture recorded:", data);
        fetchAttendance(userId);
        document.getElementById("extra-lecture-form").reset();
        document.getElementById("create-extra-lecture").style.display = "none";
        alert("Extra lecture added successfully.");
      })
      .catch((error) => {
        console.error("Error recording extra lecture:", error);
        alert("Could not add extra lecture. Please try again later.");
      });
  }

  // Fetch timetable data and filter for the current day
  function fetchTimetable(userId) {
    fetch(`/api/timetable?user_id=${userId}`)
      .then((response) => {
        if (!response.ok) throw new Error("Network response was not ok");
        return response.json();
      })
      .then((data) => {
        console.log("Timetable data:", data); 
        const currentDay = new Date().toLocaleDateString("en-US", {
          weekday: "long",
        });
        const todaysTimetable = data.filter(
          (entry) => entry.days === currentDay
        );

        todaysTimetable.sort((a, b) =>
          compareTimes(a.start_time, b.start_time)
        );

        displayTimetable(todaysTimetable);
      })
      .catch((error) => {
        console.error("Error fetching timetable:", error);
        alert("Could not fetch timetable records. Please try again later.");
      });
  }

  function compareTimes(timeA, timeB) {
    const [hourA, minuteA] = timeA.split(":").map(Number);
    const [hourB, minuteB] = timeB.split(":").map(Number);

    if (hourA !== hourB) {
      return hourA - hourB;
    } else {
      return minuteA - minuteB;
    }
  }

  // Display timetable data
  function displayTimetable(timetableData) {
    const timetableList = document.getElementById("timetableList");
    timetableList.innerHTML = ""; 

    if (timetableData.length === 0) {
      const noRecordsMessage = document.createElement("p");
      noRecordsMessage.textContent =
        "No timetable records available for today.";
      timetableList.appendChild(noRecordsMessage);
      return;
    }

    timetableData.forEach((entry) => {
      const entryElement = document.createElement("div");
      entryElement.classList.add("timetable-entry");

      entryElement.id = `${entry.subject}`;

      const subjectElement = document.createElement("p");
      subjectElement.textContent = `${entry.subject}`;

      let new_start_time = entry.start_time.split(":").slice(0, 2).join(":");
      let new_end_time = entry.end_time.split(":").slice(0, 2).join(":");
      const timeElement = document.createElement("p");
      timeElement.textContent = `Time: ${new_start_time} - ${new_end_time}`;

      // Calculate attendance for this entry
      const attendanceRecords =
        JSON.parse(localStorage.getItem("attendanceRecords")) || [];
      const attendanceEntry = attendanceRecords.find(
        (record) => record.id === entry.id
      );
      const attendancePercentage = attendanceEntry
        ? calculateIndividualAttendance(entry)
        : 0;

      const attendanceP = document.createElement("p");
      attendanceP.textContent = `Attendance: ${attendancePercentage}%`;

      const attendanceBtnDiv = document.createElement("div");

      const markPresentBtn = document.createElement("button");
      markPresentBtn.textContent = "Mark Present";
      markPresentBtn.onclick = () =>
        markAttendance(entry.id, "present", "create", entry.subject);

      const markAbsentBtn = document.createElement("button");
      markAbsentBtn.textContent = "Mark Absent";
      markAbsentBtn.onclick = () =>
        markAttendance(entry.id, "absent", "create", entry.subject);

      const markCancelledBtn = document.createElement("button");
      markCancelledBtn.textContent = "Mark Cancelled";
      markCancelledBtn.onclick = () =>
        markAttendance(entry.id, "cancelled", "create", entry.subject);

      entryElement.appendChild(subjectElement);
      entryElement.appendChild(timeElement);
      entryElement.appendChild(attendanceP);
      attendanceBtnDiv.appendChild(markPresentBtn);
      attendanceBtnDiv.appendChild(markAbsentBtn);
      attendanceBtnDiv.appendChild(markCancelledBtn);
      entryElement.appendChild(attendanceBtnDiv);

      timetableList.appendChild(entryElement);
    });
  }
});
