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
  const createTimetableModal = document.getElementById("create-timetable");
  const timetableForm = document.getElementById("timetable-form");

  createTimetableBtn.addEventListener("click", () => {
    createTimetableModal.style.display = "block";
  });

  timetableForm.addEventListener("submit", handleFormSubmit);

  const closeCreateModalButton = document.getElementById(
    "closeCreateModalButton"
  );
  closeCreateModalButton.addEventListener("click", closeCreateModal);

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
    .filter((day) => groupedByDay[day].length > 0) 
    .sort((a, b) => {
      return daysOfWeek.indexOf(a) - daysOfWeek.indexOf(b);
    }); 

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
  entries
    .sort((a, b) => compareTimes(a.start_time, b.start_time))
    .forEach((entry) =>
      lecturesContainer.appendChild(createLectureEntry(entry))
    );

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

  let newStartTime = entry.start_time.split(":").slice(0, 2).join(":");
  let newEndTime = entry.end_time.split(":").slice(0, 2).join(":");

  const infoElement = document.createElement("p");
  infoElement.textContent = `${entry.subject} : ${newStartTime} - ${newEndTime}`;

  lectureContainer.appendChild(infoElement);

  // Edit button
  const editButton = document.createElement("button");
  editButton.textContent = "Edit";
  editButton.addEventListener("click", () => openEditModal(entry));

  // Delete button
  const deleteButton = document.createElement("button");
  deleteButton.textContent = "Delete";
  deleteButton.addEventListener("click", () => deleteTimetableEntry(entry.id));

  lectureContainer.appendChild(editButton);
  lectureContainer.appendChild(deleteButton);

  return lectureContainer;
}

function openEditModal(entry) {
  const modal = document.getElementById("Modal");
  if (modal) {
    modal.style.display = "block";

    document.getElementById("subject").value = entry.subject;
    document.getElementById("start-time").value = entry.start_time;
    document.getElementById("end-time").value = entry.end_time;

    const daysSelect = document.getElementById("days");
    Array.from(daysSelect.options).forEach((option) => {
      option.selected = entry.days.includes(option.value);
    });

    const saveChangesBtn = document.getElementById("save-changes-btn");
    saveChangesBtn.onclick = () => updateTimetableEntry(entry.id);
  } else {
    console.error("Modal element not found.");
  }
}

function closeModal() {
  const modal = document.getElementById("Modal");
  if (modal) {
    modal.style.display = "none";
  }
}

function closeCreateModal() {
  const createModal = document.getElementById("create-timetable");
  if (createModal) {
    createModal.style.display = "none"; 
    document.getElementById("timetable-form").reset(); 
  }
}

function updateTimetableEntry(entryId) {
  const updatedEntry = {
    subject: document.getElementById("subject").value,
    start_time: document.getElementById("start-time").value,
    end_time: document.getElementById("end-time").value,
    days: Array.from(document.getElementById("days").selectedOptions).map(
      (option) => option.value
    ),
  };

  fetch(`/api/timetable/${entryId}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(updatedEntry),
  })
    .then((response) => {
      if (!response.ok)
        throw new Error("Failed to update the timetable entry.");
      return response.json();
    })
    .then((updated) => {
      console.log("Timetable entry updated successfully:", updated);
      closeModal();
      fetchTimetable(getUserId());
    })
    .catch((error) => {
      console.error("Error updating timetable entry:", error);
      alert("Failed to update the timetable entry. Please try again.");
    });
}

function deleteTimetableEntry(entryId) {
  if (!confirm("Are you sure you want to delete this timetable entry?")) {
    return;
  }

  fetch(`/api/timetable/${entryId}`, {
    method: "DELETE",
  })
    .then((response) => {
      if (!response.ok)
        throw new Error("Failed to delete the timetable entry.");
      return response.json();
    })
    .then((data) => {
      console.log("Timetable entry deleted successfully:", data);
      fetchTimetable(getUserId()); 
    })
    .catch((error) => {
      console.error("Error deleting timetable entry:", error);
      alert("Failed to delete timetable entry. Please try again.");
    });
}

function recordTimetable(userId, subject, startTime, endTime, days) {
  const newEntry = {
    user_id: userId,
    subject: subject,
    start_time: startTime,
    end_time: endTime,
    days: days,
  };

  fetch("/api/timetable", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(newEntry),
  })
    .then((response) => {
      if (!response.ok)
        throw new Error("Failed to create a new timetable entry.");
      return response.json();
    })
    .then((data) => {
      console.log("New timetable entry created:", data);
      closeCreateModal(); 
      fetchTimetable(userId); 
    })
    .catch((error) => {
      console.error("Error creating new timetable entry:", error);
      alert("Failed to create new timetable entry. Please try again.");
    });
}
