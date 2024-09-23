document.addEventListener("DOMContentLoaded", function () {
    const userId = getUserId(); // Retrieve user ID from session storage

    // Function to retrieve user ID from session storage
    function getUserId() {
        return sessionStorage.getItem('userId'); // Retrieve user ID from session storage
    }

    // Check if userId is valid
    if (!userId) {
        console.error("User ID not found in session storage. User may not be logged in.");
        return; // Exit if user ID is not found
    }

    fetchTimetable(userId); // Fetch timetable records on page load

    const createTimetableBtn = document.getElementById("create-timetable-btn");
    const createTimetableSection = document.getElementById("create-timetable");
    const timetableForm = document.getElementById("timetable-form");

    // Toggle visibility of the timetable creation section
    createTimetableBtn.addEventListener("click", () => {
        createTimetableSection.style.display =
            createTimetableSection.style.display === "none" ? "block" : "none";
    });

    // Handle form submission for creating a new timetable entry
    timetableForm.addEventListener("submit", function (event) {
        event.preventDefault(); // Prevent default form submission

        const subject = document.getElementById("subject").value;
        const startTime = document.getElementById("start-time").value; // Get start time
        const endTime = document.getElementById("end-time").value; // Get end time
        const days = Array.from(
            document.getElementById("days").selectedOptions
        ).map((option) => option.value);

        recordTimetable(userId, subject, startTime, endTime, days);
    });

    // Modal event listeners
    const closeModalButton = document.getElementById("closeModalButton");
    if (closeModalButton) {
        closeModalButton.addEventListener("click", closeModal);
    } else {
        console.error("'closeModalButton' element not found.");
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
                if (data === null || !Array.isArray(data)) {
                    console.warn("No timetable records found or invalid data format.");
                    alert("No timetable records found.");
                    displayTimetable([]); // Pass an empty array to handle no records gracefully
                } else {
                    displayTimetable(data); // Display fetched timetable data
                }
            })
            .catch((error) => {
                console.error("Error fetching timetable:", error);
                alert("Could not fetch timetable records. Please try again later.");
            });
    }

    // Function to display timetable records
    function displayTimetable(timetableRecords) {
        const timetableContainer = document.getElementById("timetable-container");
        timetableContainer.innerHTML = ""; // Clear existing entries

        if (timetableRecords.length === 0) {
            const noRecordsMessage = document.createElement("p");
            noRecordsMessage.textContent = "No timetable entries available.";
            timetableContainer.appendChild(noRecordsMessage);
            return;
        }

        // Group entries by day
        const groupedByDay = {};
        timetableRecords.forEach((entry) => {
            const daysArray = Array.isArray(entry.days) ? entry.days : [entry.days];
            daysArray.forEach((day) => {
                if (!groupedByDay[day]) {
                    groupedByDay[day] = [];
                }
                groupedByDay[day].push(entry);
            });
        });

        // Create day sections
        Object.keys(groupedByDay).forEach((day) => {
            const daySection = document.createElement("div");
            daySection.classList.add("day-section");
            const dayHeading = document.createElement("h3");
            dayHeading.textContent = day;
            daySection.appendChild(dayHeading);
            const lecturesContainer = document.createElement("div");
            lecturesContainer.classList.add("lectures-container");

            groupedByDay[day].forEach((entry) => {
                let new_start_time = entry.start_time.split(":").slice(0, 2).join(":");
                let new_end_time = entry.end_time.split(":").slice(0, 2).join(":");

                const lectureContainer = document.createElement("div");
                lectureContainer.classList.add("lecture-container");

                // Directly display subject and time without headings
                const infoElement = document.createElement("p");
                infoElement.textContent = `${entry.subject} : ${new_start_time} - ${new_end_time}`;

                // Create edit button
                const editButton = document.createElement("button");
                editButton.textContent = "Edit";
                editButton.classList.add("edit-button");
                editButton.dataset.id = entry.id; // Store entry ID for editing

                editButton.addEventListener("click", () => {
                    openEditModal(entry);
                });

                // Create delete button
                const deleteButton = document.createElement("button");
                deleteButton.textContent = "Delete";
                deleteButton.classList.add("delete-button");
                deleteButton.dataset.id = entry.id; // Store entry ID for deletion

                deleteButton.addEventListener("click", () => {
                    deleteTimetableEntry(entry.id, userId);
                });

                lectureContainer.appendChild(infoElement); // Append info element to lecture container
                lectureContainer.appendChild(editButton);   // Append edit button to lecture container
                lectureContainer.appendChild(deleteButton);  // Append delete button to lecture container
                lecturesContainer.appendChild(lectureContainer);
            });

            daySection.appendChild(lecturesContainer);
            timetableContainer.appendChild(daySection);
        });
    }

    // Function to record a new timetable entry
    function recordTimetable(userId, subject, startTime, endTime, days) {
        fetch("/api/timetable", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                user_id: userId,
                subject: subject,
                start_time: startTime,
                end_time: endTime,
                days: days,
            }),
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
            fetchTimetable(userId); // Refresh timetable records after adding a new entry
            document.getElementById("create-timetable").style.display = "none"; // Hide the form after submission
            alert("Timetable entry added successfully.");
        })
        .catch((error) => {
            console.error("Error recording timetable:", error);
            alert("Could not add timetable entry. Please try again later.");
        });
    }

    // Function to delete a timetable entry by ID
    function deleteTimetableEntry(entryId, userId) {
        fetch(`/api/timetable/${entryId}`, { method: 'DELETE' })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            console.log(data.message);
            fetchTimetable(userId); // Refresh the list after deletion
            alert('Timetable entry deleted successfully.');
        })
        .catch(error => {
            console.error('Error deleting timetable:', error);
            alert('Could not delete the timetable entry. Please try again later.');
        });
    }

    // Function to open the modal for editing a timetable entry
    function openEditModal(entry) {
        const modal = document.getElementById("myModal"); 
        if (modal) {
          modal.style.display = "block"; // Show the modal

          // Populate modal fields with current entry data for editing
          document.getElementById('subject').value = entry.subject;
          document.getElementById('start-time').value = entry.start_time;
          document.getElementById('end-time').value = entry.end_time;

          const daysSelect = document.getElementById('days');
          Array.from(daysSelect.options).forEach(option => {
              option.selected = entry.days.includes(option.value);  // Select previously chosen days
          });

          const saveChangesBtn = document.getElementById('save-changes-btn');
          saveChangesBtn.onclick = () => updateTimetableEntry(entry.id);  // Pass the entry ID for updating 
      } else {
          console.error('Modal element not found.');
      }
  }

  // Function to update a timetable entry by ID
  function updateTimetableEntry(entryId) {
      const subject = document.getElementById('subject').value;
      const startTime = document.getElementById('start-time').value;
      const endTime = document.getElementById('end-time').value;
      const days = Array.from(document.getElementById('days').selectedOptions).map(option => option.value);

      fetch(`/api/timetable/${entryId}`, { 
          method: 'PUT', 
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ subject, start_time: startTime, end_time: endTime, days })
      })
      .then(response => { 
          if (!response.ok) { 
              throw new Error('Network response was not ok'); 
          } 
          return response.json(); 
      }) 
      .then(data => { 
          console.log(data.message); 
          closeModal();  // Close the modal after updating 
          fetchTimetable(userId);  // Refresh the list after updating 
          alert('Timetable entry updated successfully.'); 
      }) 
      .catch(error => { 
          console.error('Error updating timetable:', error); 
          alert('Could not update the timetable entry. Please try again later.'); 
      });
  }

  // Function to close the modal
  function closeModal() {
      const modal = document.getElementById("myModal");
      if (modal) {
          modal.style.display = "none"; // Hide the modal
      }
  }
});