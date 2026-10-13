
import axios from 'axios';

const API_BASE_URL = '/api/todos';

const getAllTodos = async () => {
    const response = await axios.get(API_BASE_URL);
    return response.data;
};

const createTodo = async (todo) => {
    const response = await axios.post(API_BASE_URL, todo);
    return response.data;
};

const updateTodo = async (id, todo) => {
    const response = await axios.put(`${API_BASE_URL}/${id}`, todo);
    return response.data;
};

const deleteTodo = async (id) => {
    const response = await axios.delete(`${API_BASE_URL}/${id}`);
    return response.data;
};

const summarizeTodos = async () => {
    const response = await axios.post(`${API_BASE_URL}/summarize`);
    return response.data;
};

const todoService = {
    getAllTodos,
    createTodo,
    updateTodo,
    deleteTodo,
    summarizeTodos
};

export default todoService;
