package com.letitbee.diamondvaluationsystem.service.impl;

import com.letitbee.diamondvaluationsystem.entity.Account;
import com.letitbee.diamondvaluationsystem.entity.Customer;
import com.letitbee.diamondvaluationsystem.entity.RefreshToken;
import com.letitbee.diamondvaluationsystem.entity.Staff;
import com.letitbee.diamondvaluationsystem.enums.Role;
import com.letitbee.diamondvaluationsystem.exception.APIException;
import com.letitbee.diamondvaluationsystem.exception.CredentialsException;
import com.letitbee.diamondvaluationsystem.exception.ResourceNotFoundException;
import com.letitbee.diamondvaluationsystem.payload.*;
import com.letitbee.diamondvaluationsystem.repository.AccountRepository;
import com.letitbee.diamondvaluationsystem.repository.CustomerRepository;
import com.letitbee.diamondvaluationsystem.repository.RefreshTokenRepository;
import com.letitbee.diamondvaluationsystem.repository.StaffRepository;
import com.letitbee.diamondvaluationsystem.security.JwtTokenProvider;
import com.letitbee.diamondvaluationsystem.service.AccountService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.GenerationType;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.UUID;

@Service
public class AccountServiceImpl implements AccountService {
    private AccountRepository accountRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private AuthenticationManager authenticationManager;
    private CustomerRepository customerRepository;
    private StaffRepository staffRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private ModelMapper mapper;
    private String siteURL = "https://www.hntdiamond.store/";

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${app-jwt-expiration-refresh-token-milliseconds}")
    private long jwtExpirationRefreshDate;

    public AccountServiceImpl(AccountRepository accountRepository, ModelMapper mapper,
                              JwtTokenProvider jwtTokenProvider, AuthenticationManager authenticationManager,
                             PasswordEncoder passwordEncoder, CustomerRepository customerRepository,
                              StaffRepository staffRepository,
                              RefreshTokenRepository refreshTokenRepository) {
        this.accountRepository = accountRepository;
        this.mapper = mapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.customerRepository = customerRepository;
        this.staffRepository = staffRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }
    private AccountDTO mapToDto(Account account){
        AccountDTO accountDto = mapper.map(account, AccountDTO.class);
        return accountDto;
    }
    //convert DTO to Entity
    private Account mapToEntity(AccountDTO accountDto){
        Account account = mapper.map(accountDto, Account.class);
        return account;
    }

    @Override
    public LoginResponse login(AccountDTO accountDTO) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(accountDTO.getUsernameOrEmail(), accountDTO.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            Account account = accountRepository.findByUsernameOrEmail(accountDTO.getUsernameOrEmail(), accountDTO.getUsernameOrEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email : " + accountDTO.getUsernameOrEmail()));
            if(!account.getIs_active()){
                throw new CredentialsException(HttpStatus.FORBIDDEN, "Account has been banned or not activated yet");
            }
            LoginResponse loginResponse = new LoginResponse();
            if (account.getRole().equals(Role.CUSTOMER)) {
                Customer customer = customerRepository.findCustomerByAccount_Id(account.getId());
                loginResponse.setUserInformation(customer == null ? null : mapper.map(customer, CustomerDTO.class));
            } else {
                Staff staff = staffRepository.findStaffByAccount_Id(account.getId());
                loginResponse.setUserInformation(staff == null ? null : mapper.map(staff, StaffDTO.class));
            }
            JwtAuthResponse jwtAuthResponse = new JwtAuthResponse();
            jwtAuthResponse.setAccessToken(jwtTokenProvider.generateToken(authentication));
            String refreshToken = null;
            RefreshToken token = refreshTokenRepository.findByAccount(account)
                    .orElse(new RefreshToken());
            if(token.getToken() != null){
                refreshToken = token.getToken();
                jwtAuthResponse.setRefreshToken(refreshToken);
            }else {
                refreshToken = UUID.randomUUID().toString();
                token.setToken(refreshToken);
                long currentTimeMillis = System.currentTimeMillis();
                long expirationTimeMillis = currentTimeMillis + jwtExpirationRefreshDate;
                Date expiryDate = new Date(expirationTimeMillis);
                token.setExpiryDate(expiryDate);
                token.setAccount(account);
                refreshTokenRepository.save(token);
                jwtAuthResponse.setRefreshToken(refreshToken);
            }
            loginResponse.setUserToken(jwtAuthResponse);

            return loginResponse;
        }catch (BadCredentialsException ex) {
            throw new CredentialsException(HttpStatus.FORBIDDEN, "Invalid username or password");
        }
    }



    @Override
    public AccountResponse register(CustomerRegisterDTO customerRegisterDTO) {
        //add check for username exists in database
        if (accountRepository.existsByEmail(customerRegisterDTO.getEmail())){
            throw new APIException(HttpStatus.BAD_REQUEST, "Email is already taken");
        }else if(customerRepository.existsByPhone(customerRegisterDTO.getPhone())){
            throw new APIException(HttpStatus.BAD_REQUEST, "Phone number is already taken");
        }else if(accountRepository.existsByUsername(customerRegisterDTO.getUsername())){
            throw new APIException(HttpStatus.BAD_REQUEST, "Username is already taken");
        }

        //save account to db
        Account account = new Account();
        account.setUsername(customerRegisterDTO.getUsername());
        account.setPassword(passwordEncoder.encode(customerRegisterDTO.getPassword()));
        account.setRole(Role.CUSTOMER);
        account.setIs_active(false);
        account.setEmail(customerRegisterDTO.getEmail());
        account.setCreationDate(new Date());
        account.setVerificationCode(UUID.randomUUID().toString());
        account = accountRepository.save(account);

        //save customer to db
        Customer customer = new Customer();
        customer.setFirstName(customerRegisterDTO.getFirstName());
        customer.setLastName(customerRegisterDTO.getLastName());
        customer.setPhone(customerRegisterDTO.getPhone());
        customer.setAddress(customerRegisterDTO.getAddress());
        customer.setIdentityDocument(customerRegisterDTO.getIdentityDocument());
        customer.setAvatar(customerRegisterDTO.getAvatar());
        customer.setAccount(account);
        customerRepository.save(customer);
        //return account to client without password
        AccountResponse newAccount = new AccountResponse();
        newAccount.setId(account.getId());
        newAccount.setUsername(account.getUsername());
        newAccount.setRole(account.getRole());
        newAccount.setIs_active(account.getIs_active());
        newAccount.setEmail(account.getEmail());

        try {
            System.out.println(account.getVerificationCode());
            sendVerificationEmail(customerRegisterDTO, siteURL + "verify-account?token=" + account.getVerificationCode());
        } catch (MessagingException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return newAccount;
    }

    @Override
    public AccountResponse registerGoogle(CustomerGGRegisterDTO customerGGRegisterDTO) {
        if (accountRepository.existsByEmail(customerGGRegisterDTO.getEmail())){
            throw new APIException(HttpStatus.BAD_REQUEST, "Email is already taken");
        }else if(customerRepository.existsByPhone(customerGGRegisterDTO.getPhone())){
            throw new APIException(HttpStatus.BAD_REQUEST, "Phone number is already taken");
        }

        //save account to db
        Account account = new Account();
        account.setUsername(customerGGRegisterDTO.getEmail());
        account.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        account.setRole(Role.CUSTOMER);
        account.setIs_active(false);
        account.setEmail(customerGGRegisterDTO.getEmail());
        account.setCreationDate(new Date());
        account.setVerificationCode(UUID.randomUUID().toString());
        account = accountRepository.save(account);

        //save customer to db
        Customer customer = new Customer();
        customer.setFirstName(customerGGRegisterDTO.getFirstName());
        customer.setLastName(customerGGRegisterDTO.getLastName());
        customer.setPhone(customerGGRegisterDTO.getPhone());
        customer.setAddress(customerGGRegisterDTO.getAddress());
        customer.setIdentityDocument(customerGGRegisterDTO.getIdentityDocument());
        customer.setAvatar(customerGGRegisterDTO.getAvatar());
        customer.setAccount(account);
        customerRepository.save(customer);
        //return account to client without password
        AccountResponse newAccount = new AccountResponse();
        newAccount.setId(account.getId());
        newAccount.setUsername(account.getUsername());
        newAccount.setRole(account.getRole());
        newAccount.setIs_active(account.getIs_active());
        newAccount.setEmail(account.getEmail());

        try {
            System.out.println(account.getVerificationCode());
            sendVerificationEmailV2(customerGGRegisterDTO, siteURL + "verify-account?token=" + account.getVerificationCode());
        } catch (MessagingException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return newAccount;
    }
    private void sendVerificationEmailV2(CustomerGGRegisterDTO customerRegisterDTO, String siteURL) throws MessagingException, UnsupportedEncodingException {
        GenericCustomerRegister(siteURL, customerRegisterDTO.getFirstName(), customerRegisterDTO.getLastName(), customerRegisterDTO.getEmail());
    }

    private void GenericCustomerRegister(String siteURL, String firstName, String lastName, String email) throws MessagingException, UnsupportedEncodingException {
        String subject = "Please verify your registration";
        String senderName = "H&T Diamond";
        String mailContent = "<div style=\"font-family: Arial, sans-serif; background-color: #f0f0f0;\">";
        mailContent += "<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background: url('https://foreverflawlessnews.com/wp-content/uploads/2018/02/diamond.jpeg') no-repeat center center / cover; filter: blur(8px);\">";
        mailContent += "<tr>";
        mailContent += "<td align=\"center\" valign=\"top\" style=\"padding: 50px;\">";
        mailContent += "<table width=\"50%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: rgba(255, 255, 255, 0.8); border-radius: 10px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1); text-align: left;\">";
        mailContent += "<tr><td style=\"padding: 20px;\">";
        mailContent += "<p style=\"margin: 0 0 10px; color: #000000;\">Dear " + firstName + " " + lastName + ",</p>";
        mailContent += "<p style=\"margin: 0 0 20px; color: #000000;\">Thank you for registering with H&T Diamond. Please click the link below to verify your registration</p>";
        mailContent += "<h3 style=\"margin: 0 0 20px;\"><a href=\"" + siteURL + "\" style=\"color: #0066cc; text-decoration: none;\">Verify your account</a></h3>";
        mailContent += "<p style=\"margin: 0; color: #000000;\">Thank you,<br>The H&T Diamond Team</p>";
        mailContent += "</td></tr></table></td></tr></table></div>";

        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        helper.setFrom("hntdiamond@gmail.com", senderName);
        helper.setTo(email);
        helper.setSubject(subject);
        helper.setText(mailContent, true);
        javaMailSender.send(message);
    }


    private void sendVerificationEmail(CustomerRegisterDTO customerRegisterDTO, String siteURL) throws MessagingException, UnsupportedEncodingException {
        GenericCustomerRegister(siteURL, customerRegisterDTO.getFirstName(), customerRegisterDTO.getLastName(), customerRegisterDTO.getEmail());
    }

    @Override
    public AccountResponse registerStaff(StaffRegisterDTO staffRegisterDTO) {
        if (accountRepository.existsByEmail(staffRegisterDTO.getEmail())){
            throw new APIException(HttpStatus.BAD_REQUEST, "Email is already taken");
        }else if(customerRepository.existsByPhone(staffRegisterDTO.getPhone())){
            throw new APIException(HttpStatus.BAD_REQUEST, "Phone number is already taken");
        }else if(accountRepository.existsByUsername(staffRegisterDTO.getUsername())){
            throw new APIException(HttpStatus.BAD_REQUEST, "Username is already taken");
        }

        //save account to db
        Account account = new Account();
        account.setUsername(staffRegisterDTO.getUsername());
        account.setPassword(passwordEncoder.encode(staffRegisterDTO.getPassword()));
        account.setRole(staffRegisterDTO.getRole());
        account.setIs_active(true);
        account.setEmail(staffRegisterDTO.getEmail());
        account.setCreationDate(new Date());
        account = accountRepository.save(account);

        //save customer to db
        Staff staff = new Staff();
        staff.setFirstName(staffRegisterDTO.getFirstName());
        staff.setLastName(staffRegisterDTO.getLastName());
        staff.setPhone(staffRegisterDTO.getPhone());
        staff.setExperience(staffRegisterDTO.getExperience());
        staff.setCertificateLink(staffRegisterDTO.getCertificateLink());
        staff.setAvatar(staffRegisterDTO.getAvatar());
        staff.setAccount(account);
        staffRepository.save(staff);

        //return account to client without password
        AccountResponse newAccount = new AccountResponse();
        newAccount.setId(account.getId());
        newAccount.setUsername(account.getUsername());
        newAccount.setRole(account.getRole());
        newAccount.setIs_active(account.getIs_active());
        newAccount.setEmail(account.getEmail());
        return newAccount;
    }

    @Override
    public AccountResponse changePassword(AccountUpdate accountUpdate, Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", String.valueOf(id)));
        if(accountUpdate.getOldPassword() != null && !passwordEncoder.matches(accountUpdate.getOldPassword(), account.getPassword())){
            throw new APIException(HttpStatus.BAD_REQUEST, "Incorrect old password");
        }else if(accountUpdate.getNewPassword() != null && passwordEncoder.matches(accountUpdate.getNewPassword(), account.getPassword())){
            throw new APIException(HttpStatus.BAD_REQUEST, "New password must be different from old password");
        }
        else if(accountUpdate.getOldPassword() != null
                && passwordEncoder.matches(accountUpdate.getOldPassword(), account.getPassword())) {
            account.setPassword(passwordEncoder.encode(accountUpdate.getNewPassword()));
        }
        accountRepository.save(account);
        AccountResponse accountUpdateResponse = new AccountResponse();
        accountUpdateResponse.setId(account.getId());
        accountUpdateResponse.setUsername(account.getUsername());
        accountUpdateResponse.setEmail(account.getEmail());
        accountUpdateResponse.setIs_active(account.getIs_active());
        accountUpdateResponse.setRole(account.getRole());
        return accountUpdateResponse;
    }

    @Override
    public void forgetPassword(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "email", email));
        account.setVerificationCode(UUID.randomUUID().toString());
        accountRepository.save(account);
        try {
            sendResetEmail(mapper.map(account, AccountDTO.class), siteURL + "reset-password?token="  + account.getVerificationCode());
        } catch (MessagingException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void resetPassword(String code, String newPassword) {
        Account account = accountRepository.findByVerificationCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "code", code));
        System.out.println(newPassword);
        account.setPassword(passwordEncoder.encode(newPassword));
        account.setVerificationCode(null);
        accountRepository.save(account);
    }

    private void sendResetEmail(AccountDTO accountDTO, String siteURL) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom("hntdiamond@gmail.com", "H&T Diamond");
        helper.setTo(accountDTO.getEmail());

        String subject = "Here's the link to reset your password";

        String content = "<p>Hello,</p>"
                + "<p>You have requested to reset your password.</p>"
                + "<p>Click the link below to change your password:</p>"
                + "<p><a href=\"" + siteURL + "\">Change my password</a></p>"
                + "<br>"
                + "<p>Ignore this email if you do remember your password, "
                + "or you have not made the request.</p>";

        helper.setSubject(subject);

        helper.setText(content, true);

        javaMailSender.send(message);
    }



    @Override
    public JwtAuthResponse refreshToken(RefreshToken refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken.getToken())
                .orElseThrow(() -> new APIException(HttpStatus.BAD_REQUEST, "Invalid refresh token"));
        if(token.getExpiryDate().compareTo(new Date()) < 0){
            throw new APIException(HttpStatus.BAD_REQUEST, "Refresh token is expired");
        }
        JwtAuthResponse jwtAuthResponse = new JwtAuthResponse();
        jwtAuthResponse.setAccessToken(jwtTokenProvider.generateTokenWithUsername(token.getAccount().getUsername()));
        String refreshToken1 = UUID.randomUUID().toString();
        jwtAuthResponse.setRefreshToken(refreshToken1);
        token.setToken(refreshToken1);
        long currentTimeMillis = System.currentTimeMillis();
        long expirationTimeMillis = currentTimeMillis + jwtExpirationRefreshDate;
        Date expiryDate = new Date(expirationTimeMillis);
        token.setExpiryDate(expiryDate);
        refreshTokenRepository.save(token);
        return jwtAuthResponse;
    }

    @Override
    public void verifyAccount(String code) {
        Account account = accountRepository.findByVerificationCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "code", code));
        account.setVerificationCode(null);
        account.setIs_active(true);
        accountRepository.save(account);
    }

    @Override
    public LoginResponse findAccountByEmail(String email) {
        try {
            Account account = accountRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
            if(!account.getIs_active()) {
                throw new CredentialsException(HttpStatus.FORBIDDEN, "Account has been banned or not activated yet");
            }
            // Create Authentication object without checking the password because Google has already authenticated the user
            Authentication authentication = new UsernamePasswordAuthenticationToken(account.getEmail(), null, account.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            LoginResponse loginResponse = new LoginResponse();
            Customer customer = customerRepository.findCustomerByAccount_Id(account.getId());
            loginResponse.setUserInformation(customer == null ? null : mapper.map(customer, CustomerDTO.class));
            JwtAuthResponse jwtAuthResponse = new JwtAuthResponse();
            jwtAuthResponse.setAccessToken(jwtTokenProvider.generateToken(authentication));
            System.out.println(jwtAuthResponse.getAccessToken());

            // Handle refresh token
            String refreshToken = null;
            RefreshToken token = refreshTokenRepository.findByAccount(account)
                    .orElse(new RefreshToken());
            if (token.getToken() != null) {
                refreshToken = token.getToken();
                jwtAuthResponse.setRefreshToken(refreshToken);
            } else {
                refreshToken = UUID.randomUUID().toString();
                token.setToken(refreshToken);
                long currentTimeMillis = System.currentTimeMillis();
                long expirationTimeMillis = currentTimeMillis + jwtExpirationRefreshDate;
                Date expiryDate = new Date(expirationTimeMillis);
                token.setExpiryDate(expiryDate);
                token.setAccount(account);
                refreshTokenRepository.save(token);
                jwtAuthResponse.setRefreshToken(refreshToken);
            }
            loginResponse.setUserToken(jwtAuthResponse);

            return loginResponse;
        } catch (UsernameNotFoundException ex) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }
    }

    @Override
    public void logout(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", String.valueOf(id)));
        RefreshToken token = refreshTokenRepository.findByAccount(account)
                .orElseThrow(() -> new ResourceNotFoundException("RefreshToken", "account", String.valueOf(account.getId())));
        token.setToken(null);
        token.setExpiryDate(null);
        refreshTokenRepository.save(token);
    }

}