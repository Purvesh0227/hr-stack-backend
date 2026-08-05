import { useEffect, useState } from "react";
import Navbar from "../components/Navbar";
import "../styles/Dashboard.css";

function Dashboard() {

    const [employee, setEmployee] = useState(null);

    useEffect(() => {

        const data = localStorage.getItem("employee");

        if (data) {
            setEmployee(JSON.parse(data));
        }

    }, []);

    if (!employee) {
        return <h2>Loading...</h2>;
    }

    return (

        <>
            <Navbar />

            <div className="dashboard-container">

                <div className="welcome-card">

                    <h1>
                        Welcome, {employee.firstName} {employee.lastName}
                    </h1>

                    <p>
                        You have successfully logged in.
                    </p>

                </div>

                <div className="details-card">

                    <h2>Employee Details</h2>

                    <div className="detail-row">
                        <span>Employee ID</span>
                        <span>{employee.id}</span>
                    </div>

                    <div className="detail-row">
                        <span>Email</span>
                        <span>{employee.email}</span>
                    </div>

                    <div className="detail-row">
                        <span>Mobile</span>
                        <span>{employee.mobile}</span>
                    </div>

                </div>

            </div>

        </>

    );

}

export default Dashboard;