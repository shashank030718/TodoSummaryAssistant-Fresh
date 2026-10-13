
import React, { useState, useEffect, useCallback } from 'react';
import TodoForm from './components/TodoForm';
import TodoList from './components/TodoList';
import Notification from './components/Notification';
import todoService from './services/todoService';
import './styles/App.css';

function App() {
  const [todos, setTodos] = useState([]);
  const [editingTodo, setEditingTodo] = useState(null);
  const [notification, setNotification] = useState({
    message: '',
    type: '',
  });

  const showNotification = useCallback((message, type) => {
    setNotification({ message, type });

    setTimeout(() => {
      setNotification({ message: '', type: '' });
    }, 5000);
  }, []);

  const fetchTodos = useCallback(async () => {
    try {
      const data = await todoService.getAllTodos();
      setTodos(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Error fetching todos:', error);
      showNotification('Error fetching todos.', 'error');
    }
  }, [showNotification]);

  useEffect(() => {
    fetchTodos();
  }, [fetchTodos]);

  const addTodo = async (todo) => {
    try {
      const data = await todoService.createTodo(todo);
      setTodos((previous) => [...previous, data]);
      showNotification('Todo added successfully!', 'success');
    } catch (error) {
      console.error('Error adding todo:', error);
      showNotification('Error adding todo.', 'error');
    }
  };

  const updateTodo = async (id, updatedTodo) => {
    try {
      const data = await todoService.updateTodo(id, updatedTodo);

      setTodos((previous) =>
        previous.map((todo) => (todo.id === id ? data : todo))
      );

      showNotification('Todo updated successfully!', 'success');
    } catch (error) {
      console.error('Error updating todo:', error);
      showNotification('Error updating todo.', 'error');
    }
  };

  const deleteTodo = async (id) => {
    try {
      await todoService.deleteTodo(id);

      setTodos((previous) =>
        previous.filter((todo) => todo.id !== id)
      );

      showNotification('Todo deleted successfully!', 'success');
    } catch (error) {
      console.error('Error deleting todo:', error);
      showNotification('Error deleting todo.', 'error');
    }
  };

  const toggleComplete = async (id, completed) => {
    const todoToUpdate = todos.find((todo) => todo.id === id);

    if (!todoToUpdate) {
      return;
    }

    const updatedTodo = { ...todoToUpdate, completed };

    try {
      const data = await todoService.updateTodo(id, updatedTodo);

      setTodos((previous) =>
        previous.map((todo) => (todo.id === id ? data : todo))
      );

      showNotification('Todo status updated!', 'success');
    } catch (error) {
      console.error('Error toggling todo completion:', error);
      showNotification('Error updating todo status.', 'error');
    }
  };

  const summarizeAndSendToSlack = async () => {
    try {
      const result = await todoService.summarizeTodos();

      if (result.summary) {
        showNotification(result.summary, 'success');

        if (result.notification === 'sent') {
          console.log('Summary sent to Slack successfully.');
        } else {
          console.log(
            'Summary generated; Slack notification skipped or failed.'
          );
        }
      } else if (result.error) {
        showNotification(result.error, 'error');
      } else {
        showNotification(
          result.message || 'Summary generated.',
          'success'
        );
      }
    } catch (error) {
      console.error('Error summarizing todos:', error);

      showNotification(
        error.response?.data?.error ||
          'Failed to generate summary. Please try again.',
        'error'
      );
    }
  };

  return (
    <div className="App">
      <h1>Todo Summary Assistant</h1>

      <Notification
        message={notification.message}
        type={notification.type}
      />

      <TodoForm
        addTodo={addTodo}
        updateTodo={updateTodo}
        editingTodo={editingTodo}
        setEditingTodo={setEditingTodo}
      />

      <button
        onClick={summarizeAndSendToSlack}
        className="summary-button"
      >
        Summarize Pending Todos &amp; Send to Slack
      </button>

      <TodoList
        todos={todos}
        deleteTodo={deleteTodo}
        setEditingTodo={setEditingTodo}
        toggleComplete={toggleComplete}
      />
    </div>
  );
}

export default App;
