package com.dapeng.seckill.utils;

import org.junit.Test;
import org.apache.commons.codec.digest.DigestUtils;



public class MD5Util {

    public static String md5(String src){
        //Return a hexadecimal string representation of the MD5 digest of the given bytes.
        return DigestUtils.md5Hex(src);
    }

    // MD5的第一次加盐的salt值，客户端和服务端一致
    private static final String salt="1a2b3c4d";


    /**
     * 对客户端输入的密码加盐（第一次加盐），得到的MD5值为表单中传输的值
     * <p>
     * 在密码的传输和存储中，总共经历了两次MD5和两次salt
     * <p>
     * 第一次：客户输入到表单中的密码会在前端经过一次MD5和加盐，即；pwd_md_1st = MD5（用户明文密码 + salt_1st）。
     * 其中，pwd_md_1st 是客户端真正接收到的密码。salt_1st在客户端和服务端都是一样的
     * <p>
     * 第二次：对客户端传递到服务器的 pwd_md_1st 再一次MD5和加盐，即；pwd_md_2nd = MD5（MD5和加盐）+ salt_2nd。
     * 其中，salt_2nd是存储在服务器端的，每个用户都有自己的salt_2nd，所以在使用salt_2nd时需要从数据库中查出
     * <p>
     * 最终存储在数据库中的用户密码实际为 pwd_md_2nd。
     *
     * @param inputPwd 用户输入的密码
     * @return Calculates the MD5 digest and returns the value as a 32 character hex string.
     */
    public static String inputPwd2FormPwd(String inputPwd){
        String str = "" + salt.charAt(0) + salt.charAt(2) + inputPwd + salt.charAt(5) + salt.charAt(4);
        return md5(str);
    }


    /**
     * 对表单中的密码md5加盐，加盐后的md5为存储在数据库中的密码md5
     * 这里也就是实际存在数据库的密码
     * @param formPwd 表单中填充的明文密码
     * @param saltDb  这里的salt是在数据库查出来的，而并非第一次加盐的盐值
     * @return
     */
    public static String formPwd2DBPwd(String formPwd, String saltDb){
        String str = "" + saltDb.charAt(0) + saltDb.charAt(2) + formPwd + saltDb.charAt(5) + saltDb.charAt(4);
        return md5(str);
    }


    /**
     * 直接输入-表单-数据库的转化
     * @param inputPwd 用户输入的密码
     * @param saltDb 随机生成的salt
     * @return 最终存在数据库的密码
     */
    public static String inputPwd2DBPwd(String inputPwd, String saltDb){
        return formPwd2DBPwd(inputPwd2FormPwd(inputPwd),saltDb);
    }

    /**
     * 测试
     */
    @Test
    public void TestMD5() {
        System.out.println(inputPwd2FormPwd("123456789"));
        //52f898c27511518b951a737149783901
        System.out.println(formPwd2DBPwd("52f898c27511518b951a737149783901", "12345678"));
        System.out.println(inputPwd2DBPwd("123456789","12345678"));
        //f5b0a5288904263ed77f1cfb6648a504
    }
}