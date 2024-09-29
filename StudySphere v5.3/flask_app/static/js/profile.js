document.addEventListener("DOMContentLoaded", function () {
    const userId = getUserId(); // Retrieve user ID from session storage
    if (!userId) {
        console.error("User ID not found in session storage. User may not be logged in.");
        return; // Exit if user ID is not found
    }

    fetchUserProfile(userId); // Fetch user profile on page load
    fetchAssignmentsSummary(userId); // Fetch assignments summary
    fetchAttendanceSummary(userId); // Fetch attendance summary
    fetchInterests(userId); // Fetch user interests

    const editProfileBtn = document.getElementById("edit-profile-btn");
    const editModal = document.getElementById("edit-profile-modal");
    const closeButton = document.querySelector(".close-button");

    // Check if button exists before adding event listener
    if (editProfileBtn) {
        editProfileBtn.addEventListener("click", () => {
            if (editModal) { // Check if modal exists
                editModal.style.display = "block"; // Show modal
                populateEditForm(); // Populate form with current user data
            } else {
                console.error("Edit Profile modal not found!");
            }
        });
    } else {
        console.error("Edit Profile button not found!");
    }

    // Close modal functionality
    if (closeButton) { 
        closeButton.addEventListener("click", () => {
            if (editModal) { // Check if modal exists before accessing style
                editModal.style.display = "none"; // Hide modal
            }
        });
    }

    // Save changes functionality
    const saveChangesBtn = document.getElementById("save-changes-btn");
    if (saveChangesBtn) {
        saveChangesBtn.addEventListener("click", () => {
            const name = document.getElementById("edit-name").value;
            const bio = document.getElementById("edit-bio").value;

            saveProfileChanges(userId, name, bio);
        });
    } else {
        console.error("Save Changes button not found!");
    }
});

// Function to retrieve user ID from session storage
function getUserId() {
    return sessionStorage.getItem("userId"); // Retrieve user ID from session storage
}

// Function to fetch user profile (remains unchanged)
function fetchUserProfile(userId) {
    fetch(`/api/profile?user_id=${userId}`)
        .then((response) => {
            if (!response.ok) {
                throw new Error("Network response was not ok");
            }
            return response.json();
        })
        .then((data) => {
            displayUserProfile(data);
        })
        .catch((error) => {
            console.error("Error fetching user profile:", error);
            alert('Could not fetch user profile. Please try again later.');
        });
}

// Function to retrieve user ID from session storage
function getUserId() {
  return sessionStorage.getItem("userId"); // Retrieve user ID from session storage
}

// Function to fetch user profile
function fetchUserProfile(userId) {
  fetch(`/api/profile?user_id=${userId}`)
      .then((response) => {
          if (!response.ok) {
              throw new Error("Network response was not ok");
          }
          return response.json();
      })
      .then((data) => {
          displayUserProfile(data);
      })
      .catch((error) => {
          console.error("Error fetching user profile:", error);
          alert('Could not fetch user profile. Please try again later.');
      });
}

// Function to display user profile
function displayUserProfile(profile) {
  document.getElementById("user-name").textContent = profile.name;
  document.getElementById("user-bio").textContent = profile.bio;
}

// Populate the edit form with current user data
function populateEditForm() {
  const currentName = document.getElementById("user-name").textContent;
  const currentBio = document.getElementById("user-bio").textContent;

  document.getElementById("edit-name").value = currentName;
  document.getElementById("edit-bio").value = currentBio;
}

// Function to save profile changes
function saveProfileChanges(userId, name, bio) {
  fetch(`/api/profile/update`, { // Adjust endpoint as necessary for your backend
      method: "POST",
      headers: {
          "Content-Type": "application/json",
      },
      body: JSON.stringify({ user_id: userId, name: name, bio: bio }),
  })
  .then((response) => {
      if (!response.ok) {
          throw new Error("Network response was not ok");
      }
      return response.json();
  })
  .then((data) => {
      alert('Profile updated successfully!');
      document.getElementById("user-name").textContent = name; // Update displayed name
      document.getElementById("user-bio").textContent = bio;   // Update displayed bio
      closeModal(); // Close the modal after saving changes
      displayUserProfile(data)
      fetchUserProfile(userId)
  })
  .catch((error) => {
      console.error("Error saving profile changes:", error);
      alert('Could not save profile changes. Please try again later.');
  });
}

// Function to close the modal after saving changes or when clicking the close button
function closeModal() {
  const editModal = document.getElementById("edit-profile-modal");
  editModal.style.display = "none"; // Hide modal
}

// Function to fetch assignments summary 
function fetchAssignmentsSummary(userId) {
 fetch(`/api/assignments/summary?user_id=${userId}`)
     .then((response) => {
         if (!response.ok) {
             throw new Error("Network response was not ok");
         }
         return response.json();
     })
     .then((data) => {
         displayAssignmentsSummary(data);
     })
     .catch((error) => {
         console.error("Error fetching assignments summary:", error);
         alert('Could not fetch assignments summary. Please try again later.');
     });
}

// Function to display assignments summary 
function displayAssignmentsSummary(summary) {
 document.getElementById("upcoming-assignments").textContent = summary.upcoming;
 document.getElementById("overdue-assignments").textContent = summary.overdue;
}

// Function to fetch attendance summary 
function fetchAttendanceSummary(userId) {
 fetch(`/api/attendance/summary?user_id=${userId}`)
     .then((response) => {
         if (!response.ok) {
             throw new Error("Network response was not ok");
         }
         return response.json();
     })
     .then((data) => {
         displayAttendanceSummary(data);
     })
     .catch((error) => {
         console.error("Error fetching attendance summary:", error);
         alert('Could not fetch attendance summary. Please try again later.');
     });
}

// Function to display attendance summary 
function displayAttendanceSummary(summary) {
 document.getElementById("classes-attended").textContent = summary.attended;
 document.getElementById("classes-missed").textContent = summary.missed;
 document.getElementById("attendance-percentage").textContent = summary.percentage + "%";
}

// Function to fetch user interests 
function fetchInterests(userId) {
 fetch(`/api/interests?user_id=${userId}`)
     .then((response) => {
         if (!response.ok) {
             throw new Error("Network response was not ok");
         }
         return response.json();
     })
     .then((data) => {
         displayInterests(data);
     })
     .catch((error) => {
         console.error("Error fetching interests:", error);
         alert('Could not fetch interests. Please try again later.');
     });
}

// Function to display user interests 
function displayInterests(interests) {
 const interestsList = document.getElementById("interests-list");
 interestsList.innerHTML = ""; // Clear existing interests

 interests.forEach((interest) => {
     const interestItem = document.createElement("li");
     interestItem.textContent = interest;
     interestsList.appendChild(interestItem);
 });
}