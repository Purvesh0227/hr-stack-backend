import { Navigate } from "react-router-dom";

function ProtectedRoute({ children }) {

    const employee = localStorage.getItem("employee");

    return employee ? children : <Navigate to="/login" />;
}

export default ProtectedRoute;