document.addEventListener('DOMContentLoaded', function() {
  const userId = getUserId(); // Get the user ID from session storage
  fetchUserProfile(userId); // Fetch user profile on page load

  const updateProfileForm = document.getElementById('update-profile-form');
  updateProfileForm.addEventListener('submit', function(event) {
      event.preventDefault();
      const name = document.getElementById('name').value;
      const email = document.getElementById('email').value;
      const bio = document.getElementById('bio').value;
      updateProfile(userId, name, email, bio);
  });
});

function fetchUserProfile(userId) {
  fetch(`/api/users/${userId}`)
      .then(response => response.json())
      .then(data => {
          displayUserProfile(data);
      })
      .catch(error => {
          console.error('Error fetching user profile:', error);
      });
}

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
      displaySuccessMessage('Profile updated successfully!');
  })
  .catch(error => {
      console.error('Error updating profile:', error);
  });
}

function displayUserProfile(userProfile) {
  const nameElement = document.getElementById('name');
  const emailElement = document.getElementById('email');
  const bioElement = document.getElementById('bio');

  nameElement.value = userProfile.name;
  emailElement.value = userProfile.email;
  bioElement.value = userProfile.bio;
}

function displaySuccessMessage(message) {
  alert(message);
}