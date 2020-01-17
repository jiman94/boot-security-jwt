package com.example.auth.Controller;

import com.example.auth.Domain.Account;
import com.example.auth.Domain.AccountRole;
import com.example.auth.Domain.Token;
import com.example.auth.Repository.AccountRepository;
import com.example.auth.jwt.JwtTokenUtil;
import com.example.auth.service.JwtUserDetailsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


@RestController
@CrossOrigin
@RequestMapping // This means URL's start with /demo (after Application path)
public class MainController {
    private Logger logger = LoggerFactory.getLogger(ApplicationRunner.class);

    @Autowired // This means to get the bean called userRepository
    // Which is auto-generated by Spring, we will use it to handle the data
    private AccountRepository accountRepository;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PasswordEncoder bcryptEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private JwtUserDetailsService userDetailsService;

    @Autowired
    private AuthenticationManager am;


    @RequestMapping(value = "/newuser/test", method = RequestMethod.POST)
    public Map<String, Object> authenTest(@RequestBody Map<String, String> m) throws Exception {
        logger.info("test input username: " + m.get("username") + ", password: " + m.get("password"));
        am.authenticate(new UsernamePasswordAuthenticationToken(m.get("username"), m.get("password")));

        final UserDetails userDetails = userDetailsService.loadUserByUsername(m.get("username"));
        final String token = jwtTokenUtil.generateToken(userDetails);
        System.out.println("test input username: " + m.get("username") + ", password: " + m.get("password"));
        System.out.println("token: " + token);
        Token tok = new Token();
        tok.setUsername(m.get("username"));
        tok.setToken(token);
        ValueOperations<String, Object> vop = redisTemplate.opsForValue();
        vop.set(m.get("username"), tok);
        Token result = (Token)vop.get(m.get("username"));
        System.out.println("result: " + result);

        //redisRepository.save(tok);
        Map<String, Object> map = new HashMap<>();
        map.put("token", token);
        return map;
    }


    @PostMapping(path="/newuser/add") // Map ONLY POST Requests
    public Map<String, Object> addNewUser (@RequestBody Account account) {
        // @ResponseBody means the returned String is the response, not a view name
        // @RequestParam means it is a parameter from the GET or POST request

        //Role 따로 클래스 만들었을 경우
        AccountRole role = new AccountRole();
        role.setRoleName("BASIC");
        account.setRoles(Arrays.asList(role));

        Map<String, Object> map = new HashMap<>();
        System.out.println("회원가입요청 아이디: "+account.getUsername() + "비번: " + account.getPassword());
        if (accountRepository.findUserByUsername(account.getUsername()) == null) {
            account.setUsername(account.getUsername());
            account.setEmail(account.getEmail());

            account.setPassword(bcryptEncoder.encode(account.getPassword()));
            map.put("success", true);
            accountRepository.save(account);
            return map;
        } else {
            map.put("success", false);
            map.put("message", "duplicated username");
        }
        return map;
    }



    @PostMapping(path="/newuser/checkemail")
    public boolean checkEmail (@RequestBody Map<String, String> m) {
        System.out.println("이메일체크 요청 이메일: " + m.get("email"));
        if (accountRepository.findUserByEmail(m.get("email")) == null) return true;
        else return false;
    }


    @GetMapping(path="/all")
    public Iterable<Account> getAllUsers() {
        return accountRepository.findAll();
    }

    @GetMapping(path="/newuser/hello")
    public String hello() {
        return "hello~";
    }

    @GetMapping(path="/admin/test")
    public String admin() {
        return "admin";
    }
}