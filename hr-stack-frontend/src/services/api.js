import axios from "axios";

const API = axios.create({
    baseURL: "http://localhost:8080/employee"
});

export const loginEmployee = (loginData) => API.post("/login", loginData);
export const registerEmployee = (employeeData) => API.post("/register", employeeData);
export const createAdmin = (adminData) => API.post("/createAdmin", adminData);
export const getAllAdmins = (email) => API.get("/allAdmins", { params: { email } });
export const getAdminProfile = (email) => API.get("/adminProfile", { params: { email } });
export const getAllEmployees = (email) => API.get("/allEmployees", { params: { email } });

export default API;