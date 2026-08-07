import { useState } from "react";
import API from "../services/api";
import Navbar from "../components/Navbar";
import AdminSidebar from "../components/AdminSidebar";
import Footer from "../components/Footer";


function Dashboard() {

    const employee = JSON.parse(localStorage.getItem("employee"));
    const role = localStorage.getItem("role");
    const email = localStorage.getItem("email");

    const [employees, setEmployees] = useState([]);
    const [admins, setAdmins] = useState([]);
    const [activeMenu, setActiveMenu] = useState("dashboard");

    const getAllEmployees = async () => {
        try {
            const response = await API.get("/allEmployees", {
                params: { email }
            });

            setEmployees(response.data);
        } catch (error) {
            alert(error.response?.data || "Unable to fetch employees");
        }
    };

    const getAllAdmins = async () => {
        try {
            const response = await API.get("/allAdmins", {
                params: { email }
            });

            setAdmins(response.data);
        } catch (error) {
            alert(error.response?.data || "Unable to fetch admins");
        }
    };

    return (
        <>
            <Navbar />

            {role === "ADMIN" ? (

                <div className="dashboard-wrapper">

                    <AdminSidebar
                        activeMenu={activeMenu}
                        setActiveMenu={setActiveMenu}
                    />

                    <main className="dashboard-main">

                        <div className="dashboard-header">
                            <div>
                                <h1>Welcome, {employee.firstName}</h1>
                                <p>Manage employees and administrators.</p>
                            </div>
                        </div>

                        {activeMenu === "dashboard" && (

                            <div className="content-card">

                                <div className="card-header">

                                    <h2>Employees</h2>

                                    <button
                                        className="primary-btn"
                                        onClick={getAllEmployees}
                                    >
                                        View Employees
                                    </button>

                                </div>

                                {employees.length > 0 && (

                                    <table className="employee-table">

                                        <thead>

                                        <tr>
                                            <th>Name</th>
                                            <th>Email</th>
                                            <th>Mobile</th>
                                            <th>Role</th>
                                        </tr>

                                        </thead>

                                        <tbody>

                                        {employees.map(emp => (

                                            <tr key={emp.id}>
                                                <td>{emp.firstName} {emp.lastName}</td>
                                                <td>{emp.email}</td>
                                                <td>{emp.mobile}</td>
                                                <td>{emp.role}</td>
                                            </tr>

                                        ))}

                                        </tbody>

                                    </table>

                                )}

                            </div>

                        )}

                        {activeMenu === "admins" && (

                            <div className="content-card">

                                <div className="card-header">

                                    <h2>Administrators</h2>

                                    <button
                                        className="primary-btn"
                                        onClick={getAllAdmins}
                                    >
                                        View Admins
                                    </button>

                                </div>

                                {admins.length > 0 && (

                                    <table className="employee-table">

                                        <thead>

                                        <tr>
                                            <th>Name</th>
                                            <th>Email</th>
                                            <th>Mobile</th>
                                            <th>Role</th>
                                        </tr>

                                        </thead>

                                        <tbody>

                                        {admins.map(admin => (

                                            <tr key={admin.id}>
                                                <td>{admin.firstName} {admin.lastName}</td>
                                                <td>{admin.email}</td>
                                                <td>{admin.mobile}</td>
                                                <td>{admin.role}</td>
                                            </tr>

                                        ))}

                                        </tbody>

                                    </table>

                                )}

                            </div>

                        )}

                        {activeMenu === "settings" && (

                            <div className="content-card">

                                <h2>Settings</h2>

                                <p>
                                    Settings page will be added in future.
                                </p>

                            </div>

                        )}

                    </main>

                </div>

            ) : (

                <div className="employee-dashboard">

                    <div className="profile-card">

                        <h2>My Profile</h2>

                        <p><strong>Employee ID:</strong> {employee.id}</p>
                        <p><strong>First Name:</strong> {employee.firstName}</p>
                        <p><strong>Last Name:</strong> {employee.lastName}</p>
                        <p><strong>Email:</strong> {employee.email}</p>
                        <p><strong>Mobile:</strong> {employee.mobile}</p>
                        <p><strong>Role:</strong> {employee.role}</p>

                    </div>

                </div>

            )}
        <Footer />
        </>
    );
}

export default Dashboard;