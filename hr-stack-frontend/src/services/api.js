import axios from "axios";

const API = axios.create({
    baseURL: "http://localhost:8080/employee"
});

// Automatically attach JWT to every request
API.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem("token");

        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Login
export const loginEmployee = (loginData) =>API.post("/login", loginData);
// Register Employee
export const registerEmployee = (employeeData) =>API.post("/register", employeeData);
// Create Admin
export const createAdmin = (adminData) => API.post("/createAdmin", adminData);
// Get All Admins
export const getAllAdmins = (email) => API.get("/allAdmins", { params: { email } });
// Get Admin Profile
export const getAdminProfile = (email) => API.get("/adminProfile", { params: { email } });
// Get All Employees
export const getAllEmployees = (email) => API.get("/allEmployees", { params: { email }});

export default API;