import { useNavigate } from "react-router-dom";
import "../styles/Navbar.css";

function Navbar() {

    const navigate = useNavigate();

    const handleLogout = () => {

        localStorage.removeItem("employee");

        navigate("/login");

    };

    return (

        <nav className="navbar">

            <h2>HR Stack</h2>

            <button onClick={handleLogout}>
                Logout
            </button>

        </nav>

    );

}

export default Navbar;