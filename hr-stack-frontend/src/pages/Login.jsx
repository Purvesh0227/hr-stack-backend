import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import API from "../services/api";


function Login() {

    const navigate = useNavigate();
    const [loginData, setLoginData] = useState({
        email: "",
        password: ""
    });

    const handleChange = (e) => {
        setLoginData({
            ...loginData,
            [e.target.name]: e.target.value
        });
    };

    //run when we are going to press login button 
    const handleLogin = async (e) => {

        e.preventDefault();
        try {
            const response = await API.post("/login", loginData);
            localStorage.setItem("employee",JSON.stringify(response.data));
            localStorage.setItem("email",response.data.email);
            localStorage.setItem("role",response.data.role);

            alert("Login Successful");
            navigate("/dashboard");

        } catch (error) {

            alert(
                error.response?.data?.error || "Invalid Credentials"
            );
        }
    };

    return (

        <div className="login-container">
            <form className="login-card" onSubmit={handleLogin}>
                <h2>Employee Login</h2>
                <input
                    type="email"
                    name="email"
                    placeholder="Enter Email"
                    onChange={handleChange}
                    required
                />
                <input
                    type="password"
                    name="password"
                    placeholder="Enter Password"
                    onChange={handleChange}
                    required
                />

                <button type="submit">
                    Login
                </button>

                <p>Don't have an account?
                    <Link to="/register">
                        Register
                    </Link>
                </p>
            </form>
        </div>
    );
}

export default Login;