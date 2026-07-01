const form = document.querySelector("#add-task-form");
const taskInput = document.querySelector("#task-input");
const taskList = document.querySelector("#task-list");
const totalCount = document.querySelector("#total-count");
const pendingCount = document.querySelector("#pending-count");
const completedCount = document.querySelector("#completed-count");
const openCount = document.querySelector("#open-count");
const apiBaseUrl = (window.API_BASE_URL || "https://to-do-list-53ez.onrender.com").replace(/\/$/, "");

function buildApiUrl(path) {
    return `${apiBaseUrl}${path}`;
}

async function requestJson(url, options = {}) {
    const response = await fetch(url, {
        headers: {
            "Content-Type": "application/json",
            ...options.headers
        },
        ...options
    });

    if (!response.ok) {
        throw new Error(`Request failed with status ${response.status}`);
    }

    return response.status === 204 ? null : response.json();
}

async function loadTasks() {
    const tasks = await requestJson(buildApiUrl("/api/tasks"));
    renderTasks(tasks);
}

function renderTasks(tasks) {
    const completed = tasks.filter((task) => task.completed).length;
    const pending = tasks.length - completed;

    totalCount.textContent = tasks.length;
    pendingCount.textContent = pending;
    completedCount.textContent = completed;
    openCount.textContent = `${pending} still open`;

    if (tasks.length === 0) {
        taskList.innerHTML = "<div class=\"empty\">No tasks yet. Add one above to start your list.</div>";
        return;
    }

    const list = document.createElement("ul");
    tasks.forEach((task) => {
        const item = document.createElement("li");
        item.className = task.completed ? "complete" : "pending";

        const check = document.createElement("span");
        check.className = "check";
        check.textContent = task.completed ? "✓" : "•";

        const text = document.createElement("span");
        text.className = "task-text";
        text.textContent = task.name;

        const actions = document.createElement("div");
        actions.className = "task-actions";

        const button = document.createElement("button");
        button.type = "button";

        if (task.completed) {
            button.className = "remove-btn";
            button.textContent = "Remove";
            button.addEventListener("click", () => deleteTask(task.id));
        } else {
            button.className = "done-btn";
            button.textContent = "Done";
            button.addEventListener("click", () => completeTask(task.id));
        }

        actions.append(button);
        item.append(check, text, actions);
        list.append(item);
    });

    taskList.replaceChildren(list);
}

async function addTask(taskName) {
    await requestJson(buildApiUrl("/api/tasks"), {
        method: "POST",
        body: JSON.stringify({ task: taskName })
    });
    taskInput.value = "";
    await loadTasks();
}

async function completeTask(id) {
    await requestJson(buildApiUrl(`/api/tasks/${id}/complete`), { method: "POST" });
    await loadTasks();
}

async function deleteTask(id) {
    await requestJson(buildApiUrl(`/api/tasks/${id}`), { method: "DELETE" });
    await loadTasks();
}

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const taskName = taskInput.value.trim();
    if (taskName.length > 0) {
        await addTask(taskName);
    }
});

loadTasks().catch(() => {
    taskList.innerHTML = "<div class=\"empty\">Unable to load tasks. Start the backend and refresh this page.</div>";
});
