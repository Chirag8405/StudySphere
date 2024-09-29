document.addEventListener("DOMContentLoaded", init);

function init() {
  const userId = getUserId();

  if (!userId) {
    console.error(
      "User ID not found in session storage. User may not be logged in."
    );
    return;
  }

  fetchTimetable(userId);
  setupEventListeners();
}

function getUserId() {
  return sessionStorage.getItem("userId");
}

function setupEventListeners() {
  const createTimetableBtn = document.getElementById("create-timetable-btn");
  const createTimetableSection = document.getElementById("create-timetable");
  const timetableForm = document.getElementById("timetable-form");

  createTimetableBtn.addEventListener("click", () => {
    createTimetableSection.style.display =
      createTimetableSection.style.display === "none" ? "block" : "none";
  });

  timetableForm.addEventListener("submit", handleFormSubmit);

  const closeModalButton = document.getElementById("closeModalButton");
  if (closeModalButton) {
    closeModalButton.addEventListener("click", closeModal);
  } else {
    console.error("'closeModalButton' element not found.");
  }
}

function handleFormSubmit(event) {
  event.preventDefault();

  const subject = document.getElementById("subject").value;
  const startTime = document.getElementById("start-time").value;
  const endTime = document.getElementById("end-time").value;

  const days = Array.from(document.getElementById("days").selectedOptions).map(
    (option) => option.value
  );

  recordTimetable(getUserId(), subject, startTime, endTime, days);
}

function fetchTimetable(userId) {
  fetch(`/api/timetable?user_id=${userId}`)
    .then((response) => {
      if (!response.ok) throw new Error("Network response was not ok");
      return response.json();
    })
    .then((data) => {
      if (!Array.isArray(data) || data.length === 0) {
        console.warn("No timetable records found or invalid data format.");
        alert("No timetable records found.");
        displayTimetable([]);
      } else {
        displayTimetable(data);
      }
    })
    .catch((error) => {
      console.error("Error fetching timetable:", error);
      alert("Could not fetch timetable records. Please try again later.");
    });
}

const daysOfWeek = [
  "Sunday",
  "Monday",
  "Tuesday",
  "Wednesday",
  "Thursday",
  "Friday",
  "Saturday",
];

function displayTimetable(timetableRecords) {
  const timetableContainer = document.getElementById("timetable-container");
  timetableContainer.innerHTML = "";

  if (timetableRecords.length === 0) {
    const noRecordsMessage = document.createElement("p");
    noRecordsMessage.textContent = "No timetable entries available.";
    timetableContainer.appendChild(noRecordsMessage);
    return;
  }

  const groupedByDay = groupEntriesByDay(timetableRecords);

  // Sort days based on their order in the week
  const sortedDays = Object.keys(groupedByDay)
    .filter((day) => groupedByDay[day].length > 0) // Only include days with lectures
    .sort((a, b) => {
      return daysOfWeek.indexOf(a) - daysOfWeek.indexOf(b);
    }); // Sort the days in ascending order

  // Create sections for each day
  sortedDays.forEach((day) => {
    const daySection = createDaySection(day, groupedByDay[day]);
    timetableContainer.appendChild(daySection);
  });
}

function groupEntriesByDay(timetableRecords) {
  return timetableRecords.reduce((acc, entry) => {
    const daysArray = Array.isArray(entry.days) ? entry.days : [entry.days];

    daysArray.forEach((day) => {
      if (!acc[day]) acc[day] = [];
      acc[day].push(entry);
    });

    return acc;
  }, {});
}

function createDaySection(day, entries) {
  const daySection = document.createElement("div");
  daySection.classList.add("day-section");

  const dayHeading = document.createElement("h3");
  dayHeading.textContent = day;

  daySection.appendChild(dayHeading);

  const lecturesContainer = document.createElement("div");
  lecturesContainer.classList.add("lectures-container");

  // Sort entries by start time in ascending order
  entries.sort((a, b) => compareTimes(a.start_time, b.start_time))
         .forEach(entry => lecturesContainer.appendChild(createLectureEntry(entry)));

  daySection.appendChild(lecturesContainer);

  return daySection;
}

// Compare two time strings "HH:MM" format
function compareTimes(timeA, timeB) {
  const [hourA, minuteA] = timeA.split(":").map(Number);
  const [hourB, minuteB] = timeB.split(":").map(Number);

  if (hourA !== hourB) {
    return hourA - hourB;
  } else {
    return minuteA - minuteB;
  }
}

function createLectureEntry(entry) {
  const lectureContainer = document.createElement("div");
  lectureContainer.classList.add("lecture-container");

  // Format start and end times
  let newStartTime = entry.start_time.split(":").slice(0, 2).join(":");
  let newEndTime = entry.end_time.split(":").slice(0, 2).join(":");

  const infoElement = document.createElement("p");
  infoElement.textContent = `${entry.subject} : ${newStartTime} - ${newEndTime}`;

  lectureContainer.appendChild(infoElement);

  // Create edit button
  const editButton = createButton("Edit", "edit-button", () =>
    openEditModal(entry)
  );

  // Create delete button
  const deleteButton = createButton("Delete", "delete-button", () =>
    deleteTimetableEntry(entry.id)
  );

  lectureContainer.appendChild(editButton);
  lectureContainer.appendChild(deleteButton);

  return lectureContainer;
}

function createButton(text, className, onClick) {
  const button = document.createElement("button");
  button.textContent = text;
  button.classList.add(className);
  button.addEventListener("click", onClick);
  return button;
}

function recordTimetable(userId, subject, startTime, endTime, days) {
  fetch("/api/timetable", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      user_id: userId,
      subject,
      start_time: startTime,
      end_time: endTime,
      days,
    }),
  })
    .then((response) => {
      if (!response.ok) throw new Error("Network response was not ok");
      return response.json();
    })
    .then((data) => {
      console.log(data.message);
      resetForm();
      fetchTimetable(userId); // Refresh timetable records after adding a new entry
      alert("Timetable entry added successfully.");
    })
    .catch((error) => {
      console.error("Error recording timetable:", error);
      alert("Could not add timetable entry. Please try again later.");
    });
}

function resetForm() {
  document.getElementById("timetable-form").reset(); // Reset the form
  document.getElementById("create-timetable").style.display = "none"; // Hide the form after submission
}

function deleteTimetableEntry(entryId) {
  fetch(`/api/timetable/${entryId}`, { method: "DELETE" })
    .then((response) => {
      if (!response.ok) throw new Error("Network response was not ok");
      return response.json();
    })
    .then((data) => {
      console.log(data.message);
      fetchTimetable(getUserId()); // Refresh the list after deletion
      alert("Timetable entry deleted successfully.");
    })
    .catch((error) => {
      console.error("Error deleting timetable:", error);
      alert("Could not delete the timetable entry. Please try again later.");
    });
}

function openEditModal(entry) {
  const modal = document.getElementById("myModal");
  if (modal) {
    modal.style.display = "block"; // Show the modal

    // Populate modal fields with current entry data for editing
    document.getElementById("subject").value = entry.subject;
    document.getElementById("start-time").value = entry.start_time;
    document.getElementById("end-time").value = entry.end_time;

    const daysSelect = document.getElementById("days");
    Array.from(daysSelect.options).forEach(
      (option) => (option.selected = entry.days.includes(option.value))
    );

    const saveChangesBtn = document.getElementById("save-changes-btn");
    saveChangesBtn.onclick = () => updateTimetableEntry(entry.id);
  } else {
    console.error("Modal element not found.");
  }
}

function updateTimetableEntry(entryId) {
  const subject = document.getElementById("subject").value;
  const startTime = document.getElementById("start-time").value;
  const endTime = document.getElementById("end-time").value;
  const days = Array.from(document.getElementById("days").selectedOptions).map(
    (option) => option.value
  );

  fetch(`/api/timetable/${entryId}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      subject,
      start_time: startTime,
      end_time: endTime,
      days,
    }),
  })
    .then((response) => {
      if (!response.ok) throw new Error("Network response was not ok");
      return response.json();
    })
    .then((data) => {
      console.log(data.message);
      closeModal();
      fetchTimetable(getUserId());
      alert("Timetable entry updated successfully.");
    })
    .catch((error) => {
      console.error("Error updating timetable:", error);
      alert("Could not update the timetable entry. Please try again later.");
    });
}

function closeModal() {
  const modal = document.getElementById("myModal");
  if (modal) modal.style.display = "none"; // Hide the modal
}
