document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('login-form');
    const signupForm = document.getElementById('signup-form');
    const loginFormContainer = document.getElementById('login-form-container');
    const signupFormContainer = document.getElementById('signup-form-container');

    document.getElementById('show-signup').addEventListener('click', function(event) {
        event.preventDefault();
        loginFormContainer.classList.add('hidden');
        signupFormContainer.classList.remove('hidden');
    });

    document.getElementById('show-login').addEventListener('click', function(event) {
        event.preventDefault();
        signupFormContainer.classList.add('hidden');
        loginFormContainer.classList.remove('hidden');
    });

    loginForm.addEventListener('submit', function(event) {
        event.preventDefault();
        
        const email = document.getElementById('login-email').value;
        const password = document.getElementById('login-password').value;

        fetch('/api/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ email, password })
        })
        .then(response => {
            if (!response.ok) {
                alert('Invalid Email or Password!')

                throw new Error('Login failed');
            }
            return response.json();
        })
        .then(data => {
            if (data.message === 'Login successful!') {
                sessionStorage.setItem('userId', data.user_id);
                window.location.href = '/index';
            } else {
                alert('Login failed: ' + data.message);
            }
        })
        .catch(error => {
            console.error('Error during login:', error);
        });
    });

    signupForm.addEventListener('submit', function(event) {
        event.preventDefault();
        
        const name = document.getElementById('signup-name').value;
        const email = document.getElementById('signup-email').value;
        const password = document.getElementById('signup-password').value;

        fetch('/api/signup', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ name, email, password })
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Signup failed');
            }
            return response.json();
        })
        .then(data => {
            if (data.message === 'User registered successfully!') {
                alert('Signup successful! You can now log in.');
                signupFormContainer.classList.add('hidden');
                loginFormContainer.classList.remove('hidden');
            } else {
                alert('Signup failed: ' + data.message);
            }
        })
        .catch(error => {
            console.error('Error during signup:', error);
        });
    });
});