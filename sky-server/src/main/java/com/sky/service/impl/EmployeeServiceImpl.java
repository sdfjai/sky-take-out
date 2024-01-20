package com.sky.service.impl;

import ch.qos.logback.classic.spi.LoggerContextListener;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.dto.PasswordEditDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordEditFailedException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        // 进行md5加密，然后再进行比对
        //DigestUtils.md5Hex(password);直接对字符串加密，使用场景：对用户的明文数据加密保存到数据库
        //DigestUtils.md5DigestAsHex(password.getBytes()) 场景：从数据库中获取数据加密
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    public void save(EmployeeDTO employeeDTO) {
        Employee employee=new Employee();
        BeanUtils.copyProperties(employeeDTO,employee);
        //默认为1正常，0为锁定
        employee.setStatus(StatusConstant.ENABLE);
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));
//        employee.setCreateTime(LocalDateTime.now());
//        employee.setUpdateTime(LocalDateTime.now());
        //当前登录的id值
//        employee.setCreateUser(BaseContext.getCurrentId());
//        employee.setUpdateUser(BaseContext.getCurrentId());
        employeeMapper.insert(employee);
    }


    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        //开始分页查询
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());
        Page<Employee> page=employeeMapper.pageQuery(employeePageQueryDTO);
        long pageTotal = page.getTotal();
        List<Employee> employeeList = page.getResult();
        PageResult pageResult = new PageResult(pageTotal,employeeList);
        return pageResult;
    }

    public void startOrStop(Integer status, Long id) {
        Employee employee = Employee.builder()
                .status(status)
                .id(id)
                .build();
        employeeMapper.update(employee);
    }

    public Employee getById(Long id) {
        Employee employee=employeeMapper.getById(id);
        return employee;
    }

    public void setEmployee(EmployeeDTO employeeDTO) {
        Employee employee=new Employee();
        BeanUtils.copyProperties(employeeDTO,employee);
//        employee.setUpdateUser(BaseContext.getCurrentId());
//        employee.setUpdateTime(LocalDateTime.now());
        employeeMapper.update(employee);
    }

    /*
     *修改员工密码
     * 注：前端无法输入，此功能无法测试
     */
    public void passwordEdit(PasswordEditDTO passwordEditDTO) {
        Employee employee=new Employee();
        String oldPassword=employeeMapper.getById(passwordEditDTO.getEmpId()).getPassword();
        String newPassword=passwordEditDTO.getNewPassword();
        if (!DigestUtils.md5DigestAsHex(passwordEditDTO.getOldPassword().getBytes()).equals(oldPassword)){
            throw new PasswordEditFailedException(MessageConstant.PASSWORD_ERROR);
        }else {
            employee.setPassword(DigestUtils.md5DigestAsHex(newPassword.getBytes()));
//            employee.setUpdateTime(LocalDateTime.now());
//            employee.setUpdateUser(BaseContext.getCurrentId());
            employeeMapper.update(employee);
        }
    }


}
