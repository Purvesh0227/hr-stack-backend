import { useEffect, useState } from "react";
import API from "../services/api";
import Navbar from "../components/Navbar";

function Dashboard() {

    const employee = JSON.parse(localStorage.getItem("employee"));
    const role = localStorage.getItem("role");
    const email = localStorage.getItem("email");
    const [employees, setEmployees] = useState([]);
    const getAllEmployees = async () => 
        {
        try 
        {
            const response = await API.get("/allEmployees", {params: {email: email}});
            setEmployees(response.data);
        } catch (error) 
        {
            alert(error.response.data);
        }
    };
    return (
<>
        <Navbar />
        <div className="dashboard-container">

    <h2>Welcome {employee.firstName}</h2>

    <h3>Role : {role}</h3>

    {
        role === "ADMIN" ? (

            <>
                <button className="admin-btn" onClick={getAllEmployees}>
                    View All Employees
                </button>
                {
                    employees.length > 0 &&
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
                        {
                            employees.map(emp => (
                                <tr key={emp.id}>
                                    <td>{emp.firstName} {emp.lastName}</td>
                                    <td>{emp.email}</td>
                                    <td>{emp.mobile}</td>
                                    <td>{emp.role}</td>
                                </tr>
                            ))
                        }
                        </tbody>
                    </table>
                }
            </>
        ) : (

            <div className="profile-card">
                <h3>My Profile</h3>
                <p><strong>Employee ID :</strong> {employee.id}</p>
                <p><strong>First Name :</strong> {employee.firstName}</p>
                <p><strong>Last Name :</strong> {employee.lastName}</p>
                <p><strong>Email :</strong> {employee.email}</p>
                <p><strong>Mobile :</strong> {employee.mobile}</p>
                <p><strong>Role :</strong> {employee.role}</p>
            </div>
        )
    }
</div>
</>
);
}

export default Dashboard;