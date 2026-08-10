import { useNavigate } from "react-router-dom";
import "../styles/global.css";

function Navbar() {

    const navigate = useNavigate();

    const employee = JSON.parse(localStorage.getItem("employee"));

    const logout = () => {
        localStorage.clear();
        navigate("/");
    };

    return (
        <header className="navbar">

            <div className="navbar-logo">
                <h2>HR-Stack</h2>
            </div>

            <div className="navbar-right">

                {/* <div className="user-details">
                    <span className="welcome-text">
                        Welcome, {employee.firstName}
                    </span>

                    <span className="role-badge">
                        {employee.role}
                    </span>
                </div> */}

                <button
                    className="logout-btn"
                    onClick={logout}
                >
                    Logout
                </button>

            </div>

        </header>
    );
}

export default Navbar;