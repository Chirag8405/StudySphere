document.addEventListener("DOMContentLoaded", function () {
  const userId = getUserId(); // Retrieve the user ID from session storage
  if (!userId) {
    console.error(
      "User ID not found in session storage. User may not be logged in."
    );
    return; // Exit if user ID is not found
  }

  fetchTimetable(userId); // Fetch timetable records on page load

  const createTimetableBtn = document.getElementById("create-timetable-btn");
  const createTimetableSection = document.getElementById("create-timetable");
  const timetableForm = document.getElementById("timetable-form"); // Ensure this is defined here

  createTimetableBtn.addEventListener("click", () => {
    createTimetableSection.style.display =
      createTimetableSection.style.display === "none" ? "block" : "none";
  });

  timetableForm.addEventListener("submit", function (event) {
    event.preventDefault(); // Prevent default form submission

    const subject = document.getElementById("subject").value;
    const time = document.getElementById("time").value;
    const days = Array.from(
      document.getElementById("days").selectedOptions
    ).map((option) => option.value);

    recordTimetable(userId, subject, time, days);
  });
});

// Function to retrieve user ID from session storage
function getUserId() {
  return sessionStorage.getItem("userId"); // Retrieve user ID from session storage
}

// Function to fetch timetable records
function fetchTimetable(userId) {
  fetch(`/api/timetable?user_id=${userId}`)
    .then((response) => {
      if (!response.ok) {
        throw new Error("Network response was not ok");
      }
      return response.json();
    })
    .then((data) => {
      displayTimetable(data);
    })
    .catch((error) => {
      console.error("Error fetching timetable:", error);
    });
}

// Function to record a new timetable entry
function recordTimetable(userId, subject, time, days) {
  fetch("/api/timetable", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ user_id: userId, subject, time, days }),
  })
    .then((response) => {
      if (!response.ok) {
        throw new Error("Network response was not ok");
      }
      return response.json();
    })
    .then((data) => {
      console.log(data.message);
      document.getElementById("timetable-form").reset(); // Reset the form
      fetchTimetable(userId); // Refresh timetable records
      document.getElementById("create-timetable").style.display = "none"; // Hide the form after submission
    })
    .catch((error) => {
      console.error("Error recording timetable:", error);
    });
}

// Function to display timetable records
function displayTimetable(timetable) {
  const timetableContainer = document.getElementById("timetable-container");
  timetableContainer.innerHTML = ""; // Clear existing entries

  timetable.forEach((entry) => {
    const entryElement = document.createElement("div");
    entryElement.classList.add("timetable-entry");

    const subjectElement = document.createElement("p");
    subjectElement.textContent = `Subject: ${entry.subject}`;

    const timeElement = document.createElement("p");
    timeElement.textContent = `Time: ${entry.time}`;

    const daysElement = document.createElement("p");
    daysElement.textContent = `Days: ${entry.days}`;

    entryElement.appendChild(subjectElement);
    entryElement.appendChild(timeElement);
    entryElement.appendChild(daysElement);

    timetableContainer.appendChild(entryElement);
  });
}
