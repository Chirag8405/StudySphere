document.addEventListener("DOMContentLoaded", function () {
    const assignmentForm = document.getElementById("assignment-form");
    const modifyForm = document.getElementById("modify-form");
    const assignmentsContainer = document.getElementById("assignments-container");
    const createAssignmentBtn = document.getElementById("create-assignment-btn");
    const userId = getUserId(); 

    if (!userId) {
        console.error('User ID not found in session storage. User may not be logged in.');
        return; 
    }

    // Function to retrieve user ID from session storage
    function getUserId() {
        return sessionStorage.getItem('userId'); 
    }
    
    // Fetch assignments on page load
    fetchAssignments(userId); 
    
    // Function to fetch and display assignments
    function fetchAssignments(userId) {
        fetch(`/api/assignments?user_id=${userId}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.json();
            })
            .then(assignments => {
                console.log("API Response:", assignments); 

                displayAssignments(assignments); 
            })
            .catch(error => console.error('Error fetching assignments:', error));
    }

    // Function to display assignments
    function displayAssignments(assignments) {
        assignmentsContainer.innerHTML = ''; // 
        assignments.forEach(assignment => {
            const assignmentDiv = document.createElement("div");
            const dueDate = new Date(assignment.due_date).toLocaleDateString();
            assignmentDiv.classList.add("assignment-item");
            assignmentDiv.innerHTML = `
                <p>${assignment.name} (Due: ${dueDate}) - Status: ${assignment.status}</p>
                <button class="delete-btn" data-assignment-id="${assignment.id}">Delete</button>
                <button class="complete-btn" data-assignment-id="${assignment.id}">Mark as Completed</button>
                <button class="cancel-btn" data-assignment-id="${assignment.id}">Mark as Cancelled</button>
                <button class="modify-btn" data-assignment-id="${assignment.id}" data-assignment-name="${assignment.name}" data-due-date="${assignment.due_date}">Modify</button>
            `;
            assignmentsContainer.appendChild(assignmentDiv);
        });

        addEventListenersToButtons(); 
    }

    // Function to add event listeners to buttons
    function addEventListenersToButtons() {
        const deleteBtns = document.querySelectorAll('.delete-btn');
        deleteBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                const assignmentId = btn.dataset.assignmentId;
                deleteAssignment(assignmentId);
            });
        });

        const completeBtns = document.querySelectorAll('.complete-btn');
        completeBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                const assignmentId = btn.dataset.assignmentId;
                markAsCompleted(assignmentId);
            });
        });

        const cancelBtns = document.querySelectorAll('.cancel-btn');
        cancelBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                const assignmentId = btn.dataset.assignmentId;
                markAsCancelled(assignmentId);
            });
        });

        const modifyBtns = document.querySelectorAll('.modify-btn');
        modifyBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                const assignmentId = btn.dataset.assignmentId;
                const assignmentName = btn.dataset.assignmentName;
                const dueDate = btn.dataset.dueDate;

                document.getElementById("assignment-id").value = assignmentId;
                document.getElementById("modify-name").value = assignmentName;
                document.getElementById("modify-date").value = dueDate;

                document.getElementById("modify-assignment").style.display = "block";
            });
        });
    }

    // Handle form submission for adding assignments
    assignmentForm.addEventListener("submit", function (event) {
        event.preventDefault(); 

        const name = document.getElementById("assignment-name").value;
        const dueDate = document.getElementById("due-date").value;

        fetch("/api/assignments", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ name: name, due_date: dueDate })
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            console.log(data.message);
            fetchAssignments(userId); 
            assignmentForm.reset(); 
        })
        .catch(error => console.error('Error creating assignment:', error));
    });

    // Function to delete an assignment
    function deleteAssignment(assignmentId) {
        fetch(`/api/assignments/${assignmentId}`, { method: "DELETE" })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.json();
            })
            .then(data => {
                console.log(data.message);
                fetchAssignments(userId);
            })
            .catch(error => console.error('Error deleting assignment:', error));
    }

    // Handle form submission for modifying assignments
    modifyForm.addEventListener("submit", function (event) {
        event.preventDefault(); 

        const assignmentId = document.getElementById("assignment-id").value;
        const name = document.getElementById("modify-name").value;
        const dueDate = document.getElementById("modify-date").value;

        fetch(`/api/assignments/${assignmentId}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name: name, due_date: dueDate })
        })
        .then(response => {
            if (!response.ok) { throw new Error('Network response was not ok'); }
            return response.json();
        })
        .then(data => { 
            console.log(data.message);
            fetchAssignments(userId); 
            document.getElementById("modify-assignment").style.display = "none"; 
        })
        .catch(error => console.error('Error modifying assignment:', error));
    });

    // Mark an assignment as completed
    function markAsCompleted(assignmentId) {
        fetch(`/api/assignments/${assignmentId}/complete`, { method: "PUT" })
            .then(response => { 
                if (!response.ok) { throw new Error('Network response was not ok'); } 
                return response.json(); 
            })
            .then(data => { 
                console.log(data.message); 
                fetchAssignments(userId); 
            })
            .catch(error => console.error('Error marking assignment as completed:', error));
    }

    // Mark an assignment as cancelled
    function markAsCancelled(assignmentId) {
        fetch(`/api/assignments/${assignmentId}/cancel`, { method: "PUT" })
            .then(response => { 
                if (!response.ok) { throw new Error('Network response was not ok'); } 
                return response.json(); 
            })
            .then(data => { 
                console.log(data.message); 
                fetchAssignments(userId); 
            })
            .catch(error => console.error('Error marking assignment as cancelled:', error));
    }

    // Initial fetch of assignments
    fetchAssignments(userId);

    // Show the create assignment form when the button is clicked
    createAssignmentBtn.addEventListener("click", () => {
        const createAssignmentSection = document.getElementById("create-assignment");
        createAssignmentSection.style.display = createAssignmentSection.style.display === "none" ? "block" : "none";
    });
});