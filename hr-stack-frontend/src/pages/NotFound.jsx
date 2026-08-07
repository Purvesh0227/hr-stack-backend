import {Link} from "react-router-dom";

function NotFound(){
    return (
        <div className = "not-found-container">
            <h1>404</h1>
            <h2>Page Not Found</h2>
            <p>
                The page you are looking for does not exist.
            </p>

            <Link to="/login">
                Go Back To Login
            </Link>
            
        </div>
    );
}

export default NotFound;