import {useSelector } from 'react-redux';
import { Navigate, Outlet, useLocation } from 'react-router-dom';


const ProtectedRoute = ({ children, allowedRoles}) => {
  const {user} =useSelector((state)=>state.auth);
  const location = useLocation();

  if (!user) {
    return <Navigate to="/login" state={{from:location}} replace/>
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/unauthorized" state={{from:location}} replace/>
  }

  return children
}

export default ProtectedRoute;