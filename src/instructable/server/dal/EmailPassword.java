package instructable.server.dal;

import com.google.common.base.Preconditions;
//import instructable.server.senseffect.RealCalendar;
import instructable.server.senseffect.RealEmailOperations;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.AlgorithmParameters;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Random;

/**
 * Created by Amos Azaria on 07-Aug-15.
 */
public class EmailPassword
{
    private String email;
    private String password;

    static final String emailPasswordsTable = "email_passwords";
    static final String emailCol = "email";
    static final String encryptedPasswordCol = "enc_password";
    static final String usernameCol = "userName";
    static final String saltCol = "salt";
    static final String ivCol = "iv";

    private EmailPassword(String email, String password)
    {
        this.email = email;
        this.password = password;
    }

    public String getEmail()
    {
        return email;
    }

    public String getPassword()
    {
        return password;
    }

    /**
     *
     * @param username e.g. first part of deviceId
     * @param decryptionPswd e.g. second part of deviceId (can be any string, just must be identical to the one used to encrypt).
     */
    static Optional<EmailPassword> retrieveEmailNPassword(String username, String decryptionPswd)
    {
        String email = null;
        String encryptedPassword = null;
        String salt = null;
        String iv = null;

        try (
                Connection connection = InstDataSource.getDataSource().getConnection();
                PreparedStatement pstmt = connection.prepareStatement("select " + emailCol + "," + encryptedPasswordCol + "," + saltCol + "," + ivCol + " from " + emailPasswordsTable + " where " + usernameCol + "=?");
        )
        {
            pstmt.setString(1, username);

            try (ResultSet resultSet = pstmt.executeQuery())
            {
                while (resultSet.next())
                {
                    Preconditions.checkState(email == null, "error! more than one match! can't happen, because of db-key!");
                    email = resultSet.getString(emailCol);
                    encryptedPassword = resultSet.getString(encryptedPasswordCol);
                    salt = resultSet.getString(saltCol);
                    iv = resultSet.getString(ivCol);
                }
            }
        } catch (SQLException e)
        {
            e.printStackTrace();
        }

        if (email == null || encryptedPassword == null || salt == null || iv == null)
            return Optional.empty();

        //byte[] decPswdBytes = decPswd.getBytes(StandardCharsets.UTF_8);
        //byte[] saltBytes = salt.getBytes(StandardCharsets.UTF_8);

        String decryptedPassword;
        try
        {
            byte[] cypPasswordBytes = new BASE64Decoder().decodeBuffer(encryptedPassword);
            char[] decryptionPasswordCa = decryptionPswd.toCharArray();
            byte[] saltBytes = new BASE64Decoder().decodeBuffer(salt);
            byte[] ivByes = new BASE64Decoder().decodeBuffer(iv);


        /* Derive the key, given password and salt. */
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(decryptionPasswordCa, saltBytes, 65536, 128);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(ivByes));

            decryptedPassword = new String(cipher.doFinal(cypPasswordBytes), "UTF-8");
        } catch (Exception ex)
        {
            ex.printStackTrace();
            return Optional.empty();
        }

        return Optional.of(new EmailPassword(email,decryptedPassword));
    }


    static void setEmailNPassword(String username, String decryptionPswd, String email, String orgPassword) throws Exception
    {

        //byte[] encPasswordBytes = new BASE64Decoder().decodeBuffer(encPassword);
        char[] decryptionPasswordCa = decryptionPswd.toCharArray();
        //byte[] saltBytes = new BASE64Decoder().decodeBuffer(salt);
        //byte[] ivByes =  new BASE64Decoder().decodeBuffer(iv);

        final Random r = new SecureRandom();
        byte[] saltBytes = new byte[32];
        r.nextBytes(saltBytes);

        /* Derive the key, given password and salt. */
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(decryptionPasswordCa, saltBytes, 65536, 128);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        cipher.init(Cipher.ENCRYPT_MODE, secret);

        AlgorithmParameters params = cipher.getParameters();
        byte[] ivBytes = params.getParameterSpec(IvParameterSpec.class).getIV();
        String iv = new BASE64Encoder().encode(ivBytes);

        byte[] encVal = cipher.doFinal(orgPassword.getBytes());
        String encryptedValue = new BASE64Encoder().encode(encVal);
        String encSalt = new BASE64Encoder().encode(saltBytes);


        try (
                Connection connection = InstDataSource.getDataSource().getConnection();
                //like insert, but will replace old password in case a new one is received
                PreparedStatement pstmt = connection.prepareStatement("replace into " + emailPasswordsTable + " (" + usernameCol + "," + emailCol + "," + encryptedPasswordCol + "," + saltCol + "," + ivCol + ") values (?,?,?,?,?)");
        )
        {
            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, encryptedValue);
            pstmt.setString(4, encSalt);
            pstmt.setString(5, iv);

            pstmt.executeQuery();

        } catch (SQLException e)
        {
            e.printStackTrace();
        }

    }

    /**
     *  assume already has password
     * @param username
     * @param decryptionPswd
     * @return
     */
    public static Optional<RealEmailOperations> getRealEmailOp(String username, String decryptionPswd)
    {
        Optional<EmailPassword> emailPassword = retrieveEmailNPassword(username, decryptionPswd);
        if (emailPassword.isPresent())
            return Optional.of(new RealEmailOperations(emailPassword.get().email, emailPassword.get().password, emailPassword.get().email));
        return Optional.empty();
    }

    public static Optional<RealEmailOperations> getRealEmailOp(String username, String decryptionPswd, String email, String originalPassword)
    {
        try
        {
            setEmailNPassword(username, decryptionPswd, email, originalPassword);
            return Optional.of(new RealEmailOperations(email, originalPassword, email));
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return Optional.empty();
    }

//    public static RealEmailOperations getRealEmailOp(EmailPassword emailPassword)
//    {
//        return new RealEmailOperations(emailPassword.email, emailPassword.password, emailPassword.email);
//    }
//
//    public static RealCalendar getRealCalendarOp(EmailPassword emailPassword)
//    {
//        return new RealCalendar(); //doesn't use username and password.
//    }
}
