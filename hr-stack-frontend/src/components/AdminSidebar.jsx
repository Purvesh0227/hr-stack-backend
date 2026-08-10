import { MdDashboard } from "react-icons/md";
import { FaUserShield } from "react-icons/fa";
import { IoSettingsSharp } from "react-icons/io5";
import "../styles/global.css";

function AdminSidebar({ activeMenu, setActiveMenu, role }) {

    return (
        <aside className="sidebar">

            <div className="logo-section">
                <h2>HR-Stack</h2>
                <p>
                    {role === "ADMIN"
                        ? "Admin Dashboard"
                        : "Employee Dashboard"}
                </p>
            </div>

            <nav className="sidebar-menu">

                {/* Home / Dashboard */}
                <button
                    className={activeMenu === "dashboard" ? "active" : ""}
                    onClick={() => setActiveMenu("dashboard")}
                >
                    <MdDashboard className="icon" />

                    <span>
                        {role === "ADMIN" ? "Dashboard" : "Home"}
                    </span>
                </button>

                {/* Admin menu - only for ADMIN */}
                {role === "ADMIN" && (
                    <button
                        className={activeMenu === "admins" ? "active" : ""}
                        onClick={() => setActiveMenu("admins")}
                    >
                        <FaUserShield className="icon" />
                        <span>Admins</span>
                    </button>
                )}

                {/* Settings */}
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