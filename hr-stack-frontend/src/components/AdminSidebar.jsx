import { MdDashboard } from "react-icons/md";
import { FaUserShield } from "react-icons/fa";
import { IoSettingsSharp } from "react-icons/io5";
import "../styles/Sidebar.css";

function AdminSidebar({ activeMenu, setActiveMenu }) {
    return (
        <aside className="sidebar">

            <div className="logo-section">
                <h2>HR-Stack</h2>
                <p>Admin Dashboard</p>
            </div>

            <nav className="sidebar-menu">

                <button
                    className={activeMenu === "dashboard" ? "active" : ""}
                    onClick={() => setActiveMenu("dashboard")}
                >
                    <MdDashboard className="icon" />
                    <span>Dashboard</span>
                </button>

                <button
                    className={activeMenu === "admins" ? "active" : ""}
                    onClick={() => setActiveMenu("admins")}
                >
                    <FaUserShield className="icon" />
                    <span>Admins</span>
                </button>

                <button
                    className={activeMenu === "settings" ? "active" : ""}
                    onClick={() => setActiveMenu("settings")}
                >
                    <IoSettingsSharp className="icon" />
                    <span>Settings</span>
                </button>

            </nav>

        </aside>
    );
}

export default AdminSidebar;