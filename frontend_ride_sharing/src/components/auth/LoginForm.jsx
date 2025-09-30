import {useState} from "react";
import {useNavigate} from "react-router-dom";
import {useSelector, useDispatch} from 'react-redux';
import { login, reset } from '../../features/auth/authSlice';

const LoginForm = () => {

  const [formData, setFormData] = useState({email:'',password:''});
  const {email, password} = formData;

  const navigate = useNavigate();
  const dispatch = useDispatch();

  const {user, isLoading, isError, isSuccess, message} = useSelector(
    (state)=>state.auth
  );

  useEffect(() => {
    if (isError) {
      alert(message);
    }

    if (isSuccess || user) {
      switch (user.role) {
        case 'ADMIN':
          navigate('/admin/dashboard')
          break;
        case 'DRIVER':
          navigate('/driver/dashboard')
          break;
        case 'PASSENGER':
          navigate('/passenger/dashboard')
          break;
        default:
          navigate('/')
      }
    }

    dispatch(reset());
  },  [user, isError, isSuccess, message, navigate, dispatch]);

  const onChange = (e)=>{
    setFormData((prevState)=>({
      ...prevState,
      [e.target.name]:e.target.value
    }))
  };

  const onSubmit =(e)=>{
    e.preventDefault();
    const userData ={email, password};
    dispatch(login(userData))
  }

  if (isLoading) {
    return <h3>Loading...</h3>;
  }

  return(
    <form onSubmit={onSubmit}>
      <input
      type="email"
      name="email"
      id="email"
      value={email}
      placeholder="Enter your email"
      onChange={onChange}
      required
       />

       <input
       type="password"
       name="password"
       id="password"
       placeholder="Enter password"
       onChange={onChange}
       value={password}
       required
       />
      <button type="submit">Login</button>

    </form>
  )
};

export default LoginForm;
