import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import API from "../services/api";
import "../styles/Register.css";

function Register() {

    const navigate = useNavigate();

    const [employee, setEmployee] = useState({
        firstName: "",
        lastName: "",
        email: "",
        mobile: "",
        password: ""
    });

    const handleChange = (e) => {

        setEmployee({
            ...employee,
            [e.target.name]: e.target.value
        });

    };

    const handleRegister = async (e) => {

        e.preventDefault();

        try {

            await API.post("/register", employee);

            alert("Employee Registered Successfully");

            navigate("/login");

        } catch (error) {

            if (error.response && error.response.data) {

                const errors = error.response.data;

                let message = "";

                for (let key in errors) {
                    message += errors[key] + "\n";
                }

                alert(message);

            } else {

                alert("Registration Failed");

            }

        }

    };

    return (

        <div className="register-container">

            <form
                className="register-card"
                onSubmit={handleRegister}
            >

                <h2>Employee Registration</h2>

                <input
                    type="text"
                    name="firstName"
                    placeholder="First Name"
                    value={employee.firstName}
                    onChange={handleChange}
                    required
                />

                <input
                    type="text"
                    name="lastName"
                    placeholder="Last Name"
                    value={employee.lastName}
                    onChange={handleChange}
                    required
                />

                <input
                    type="email"
                    name="email"
                    placeholder="Email"
                    value={employee.email}
                    onChange={handleChange}
                    required
                />

                <input
                    type="text"
                    name="mobile"
                    placeholder="Mobile Number"
                    value={employee.mobile}
                    onChange={handleChange}
                    required
                />

                <input
                    type="password"
                    name="password"
                    placeholder="Password"
                    value={employee.password}
                    onChange={handleChange}
                    required
                />

                <button type="submit">
                    Register
                </button>

                <p>
                    Already have an account?
                    <Link to="/login">
                        Login
                    </Link>
                </p>

            </form>

        </div>

    );

}

export default Register;