# Java To-Do List

A simple Java HTTP To-Do List application split into a standalone frontend and backend.

## Features
- Add tasks
- View tasks
- Mark tasks as completed
- Remove completed tasks
- Data is saved by the backend to `backend/tasks.txt`

## Project layout
- `frontend/` contains the browser UI: HTML, CSS, and JavaScript.
- `backend/` contains the Java HTTP server, Maven build, and task persistence.

## Run the backend and frontend together
```bash
cd backend
mvn clean package
java -jar target/todolist-project-1.0-SNAPSHOT.jar
```

Then open:
```text
http://localhost:8080
```

The Java backend serves the files from `frontend/`, so a separate frontend server is not required.

## API
- `GET /api/tasks` returns all tasks.
- `POST /api/tasks` adds a task from JSON like `{ "task": "Buy milk" }`.
- `POST /api/tasks/{id}/complete` marks a task as completed.
- `DELETE /api/tasks/{id}` removes a task.

## GitHub repository
This project is intended to be published at:
```text
https://github.com/mouneesh798/TO-DO-LIST
```
