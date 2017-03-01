package com.mySQLqueriesForWQAM;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;




public class Main {
    static final String NP = "SELECT COUNT(*) FROM asqatasun.WEB_RESOURCE_STATISTICS wrs WHERE wrs.Id_Audit = ? and wrs.Http_Status_Code = 200";
    static final String min_page_ID = "SELECT MIN(Id_Web_Resource_Statistics) FROM asqatasun.WEB_RESOURCE_STATISTICS WHERE Id_Audit = ? and Http_Status_Code = 200";
    static final String max_page_ID = "SELECT MAX(Id_Web_Resource_Statistics) FROM asqatasun.WEB_RESOURCE_STATISTICS WHERE Id_Audit = ? and Http_Status_Code = 200";
    static final String HttpStatusCode = "SELECT Http_Status_Code from asqatasun.WEB_RESOURCE_STATISTICS where Id_Web_Resource_Statistics = ?";
    static final String NT = "SELECT SUM(Nb_Failed+Nb_Passed+Nb_Nmi) FROM asqatasun.CRITERION_STATISTICS  WHERE Id_Web_Resource_Statistics = ?";
    static final String NTx = "SELECT SUM(Nb_Failed+Nb_Passed+Nb_Nmi) FROM (SELECT Nb_Failed,Nb_Passed,Nb_Nmi FROM asqatasun.CRITERION_STATISTICS cs INNER JOIN asqatasun.CRITERION cr on cs.Id_Criterion=cr.Id_criterion WHERE cs.Id_Web_Resource_Statistics = ? AND cr.Theme_Id_Theme = ?) as a";
    static final String NTxy = "SELECT SUM(Nb_Failed+Nb_Passed+Nb_Nmi) FROM (SELECT Nb_Failed,Nb_Passed,Nb_Nmi from asqatasun.CRITERION_STATISTICS cs INNER JOIN asqatasun.CRITERION cr on cs.Id_Criterion=cr.Id_criterion WHERE cs.Id_Web_Resource_Statistics= ? AND cr.Theme_Id_Theme = ? AND cs.Criterion_Result = ?) as a";
    static final String Bxyz1 = "SELECT SUM(Nb_Nmi) FROM (SELECT  Nb_Nmi  FROM asqatasun.CRITERION_STATISTICS cs inner join asqatasun.CRITERION cr ON cs.Id_Criterion=cr.Id_Criterion inner join asqatasun.TEST t on t.Id_Criterion=cr.Id_Criterion where cs.Id_Web_Resource_Statistics=? and cr.Theme_Id_Theme=? and cs.Criterion_Result=? and t.Id_Level=? group by Id_Criterion_Statistics) AS a";
    static final String Bxyz2 = "SELECT SUM(Nb_Failed) FROM (SELECT  Nb_Failed  FROM asqatasun.CRITERION_STATISTICS cs inner join asqatasun.CRITERION cr ON cs.Id_Criterion=cr.Id_Criterion inner join asqatasun.TEST t on t.Id_Criterion=cr.Id_Criterion where cs.Id_Web_Resource_Statistics=? and cr.Theme_Id_Theme=? and cs.Criterion_Result=? and t.Id_Level=? group by Id_Criterion_Statistics) AS a";
    static final String Pxyz = "SELECT sum(Nb_Passed+Nb_nmi+Nb_Failed) FROM (SELECT Nb_Passed, Nb_Nmi , Nb_Failed FROM asqatasun.CRITERION_STATISTICS cs inner join asqatasun.CRITERION cr ON cs.Id_Criterion=cr.Id_Criterion inner join asqatasun.TEST t on t.Id_Criterion=cr.Id_Criterion where cs.Id_Web_Resource_Statistics=? and cr.Theme_Id_Theme=? and cs.Criterion_Result=? and t.Id_Level=? group by Id_Criterion_Statistics) AS a";
    static final String Bxyzl1 = "SELECT SUM(Nb_Nmi) FROM (SELECT  Nb_Nmi  FROM asqatasun.CRITERION_STATISTICS cs inner join asqatasun.CRITERION cr ON cs.Id_Criterion=cr.Id_Criterion inner join asqatasun.TEST t on t.Id_Criterion=cr.Id_Criterion where cs.Id_Web_Resource_Statistics=? and cr.Theme_Id_Theme=? and cs.Criterion_Result=? and t.Id_Level=? and cr.criterion_severity= ?  group by Id_Criterion_Statistics) AS a";
    static final String Bxyzl2 = "SELECT SUM(Nb_Failed) FROM (SELECT  Nb_Failed  FROM asqatasun.CRITERION_STATISTICS cs inner join asqatasun.CRITERION cr ON cs.Id_Criterion=cr.Id_Criterion inner join asqatasun.TEST t on t.Id_Criterion=cr.Id_Criterion where cs.Id_Web_Resource_Statistics=? and cr.Theme_Id_Theme=? and cs.Criterion_Result=? and t.Id_Level=? and cr.criterion_severity= ? group by Id_Criterion_Statistics) AS a";
    static final String Pxyzl = "SELECT sum(Nb_Passed+Nb_nmi+Nb_Failed) FROM (SELECT Nb_Passed, Nb_Nmi , Nb_Failed FROM asqatasun.CRITERION_STATISTICS cs inner join asqatasun.CRITERION cr ON cs.Id_Criterion=cr.Id_Criterion inner join asqatasun.TEST t on t.Id_Criterion=cr.Id_Criterion where cs.Id_Web_Resource_Statistics=? and cr.Theme_Id_Theme = ? and cs.Criterion_Result=? and t.Id_Level=?  and cr.criterion_severity = ? group by Id_Criterion_Statistics) AS a";
    static final String UpdateDatabase="UPDATE asqatasun.WEB_RESOURCE_STATISTICS set WQAM=? where Id_Audit=? and Http_Status_Code=-1";
    static final double[] W = {0.80, 0.16, 0.04};

    public static void main(String[] args) {
        // write your code here
        for(long i=52; i<104; i++){
            if(i!=24 && i!=70)
            computeWQAM(i);
        }
        System.out.println("Finished Successfully");
    }

    public static Connection getConnection(String Url,String username,String password) throws Exception {
        // create our mysql database connection
        String myDriver = "org.gjt.mm.mysql.Driver";
        String myUrl = Url;
        Class.forName(myDriver);
        Connection conn = DriverManager.getConnection(myUrl, username, password);
        return conn;
    }

    public static float calculate_Score(double B, double P){
        double a = 0.3;
        double b = 20;
        float A;
        if(B/P<(a-100)/(a/P-100/b)){
            A=(float)(B*(-100/b)+100);
        }
        else{
            A=(float)((-a*B/P)+a);
        }
        return A;

    }


    public static void computeWQAM(Long AuditId) {
        Connection conn = null;
        String OkeanosVMUrl = "jdbc:mysql://83.212.101.228:3306/asqatasun";
        String username = "nikos";
        String password = "password";

        float WQAM;
        try {
            conn = getConnection(OkeanosVMUrl, username, password);
            PreparedStatement stmt = conn.prepareStatement(NP);
            stmt.setLong(1, AuditId);
            PreparedStatement stmt1 = conn.prepareStatement(min_page_ID);
            stmt1.setLong(1, AuditId);
            PreparedStatement stmt2 = conn.prepareStatement(max_page_ID);
            stmt2.setLong(1, AuditId);
            ResultSet rs = stmt.executeQuery();
            ResultSet rs1 = stmt1.executeQuery();
            ResultSet rs2 = stmt2.executeQuery();
            rs.next();
            rs1.next();
            rs2.next();
            long pages = rs.getLong(1);
            int min = rs1.getInt(1);
            int max = rs2.getInt(1);
            double NT_xy, N_T, NT_x, B, P;
            String Criterion_result;
            WQAM = 0;
            float Score1, Score2, Score3;
            float Score4 = 0;
            for (int i = 0; i <= max - min; i++) {
                PreparedStatement stmt0 = conn.prepareStatement(HttpStatusCode);
                stmt0.setInt(1,i+min);
                ResultSet rs0 = stmt0.executeQuery();
                rs0.next();
                int HttpStatusCode = rs0.getInt(1);
                if (HttpStatusCode != 200) continue;
                Score3 = 0;
                PreparedStatement stmt3 = conn.prepareStatement(NT);
                stmt3.setInt(1, i + min);
                ResultSet rs3 = stmt3.executeQuery();
                rs3.next();
                N_T = rs3.getDouble(1);
                if (N_T == 0) continue;
                for (int x = 0; x <= 12; x++) {
                    Score2 = 0;
                    PreparedStatement stmt4 = conn.prepareStatement(NTx);
                    stmt4.setInt(1, i + min);
                    stmt4.setInt(2, x + 44);
                    ResultSet rs4 = stmt4.executeQuery();
                    rs4.next();
                    NT_x = rs4.getDouble(1);
                    if (NT_x == 0) continue;
                    for (int y = 0; y <= 1; y++) {
                        Score1 = 0;
                        if (y == 0) {
                            Criterion_result = "NEED_MORE_INFO";
                            PreparedStatement stmt5 = conn.prepareStatement(NTxy);
                            stmt5.setInt(1, i + min);
                            stmt5.setInt(2, x + 44);
                            stmt5.setString(3, Criterion_result);
                            ResultSet rs5 = stmt5.executeQuery();
                            rs5.next();
                            NT_xy = rs5.getDouble(1);
                        } else {
                            Criterion_result = "FAILED";
                            PreparedStatement stmt5 = conn.prepareStatement(NTxy);
                            stmt5.setInt(1, i + min);
                            stmt5.setInt(2, x + 44);
                            stmt5.setString(3, Criterion_result);
                            ResultSet rs5 = stmt5.executeQuery();
                            rs5.next();
                            NT_xy = rs5.getDouble(1);
                        }
                        if (NT_xy == 0) continue;
                        for (int z = 0; z <= 2; z++) {
                            if (y == 0) {
                                PreparedStatement stmt6 = conn.prepareStatement(Bxyz1);
                                stmt6.setInt(1, i + min);
                                stmt6.setInt(2, x + 44);
                                stmt6.setString(3, Criterion_result);
                                stmt6.setInt(4, z + 1);
                                ResultSet rs6 = stmt6.executeQuery();
                                rs6.next();
                                B = rs6.getDouble(1);
                            } else {
                                PreparedStatement stmt6 = conn.prepareStatement(Bxyz2);
                                stmt6.setInt(1, i + min);
                                stmt6.setInt(2, x + 44);
                                stmt6.setString(3, Criterion_result);
                                stmt6.setInt(4, z + 1);
                                ResultSet rs6 = stmt6.executeQuery();
                                rs6.next();
                                B = rs6.getDouble(1);
                            }


                            PreparedStatement stmt7 = conn.prepareStatement(Pxyz);
                            stmt7.setInt(1, i + min);
                            stmt7.setInt(2, x + 44);
                            stmt7.setString(3, Criterion_result);
                            stmt7.setInt(4, z + 1);

                            ResultSet rs7 = stmt7.executeQuery();

                            rs7.next();

                            P = rs7.getDouble(1);
                            if (P != 0) {
                                double A = calculate_Score(B, P);
                                Score1 += W[z] * A;

                            }

                        }
                        Score2 = (float) (Score2 + ((NT_xy / NT_x) * Score1));
                    }
                    Score3 = (float) (Score3 + (NT_x / N_T * Score2));
                }
                Score4 += Score3;

            }
            WQAM = Score4 / pages;
            PreparedStatement stmt8 = conn.prepareStatement(UpdateDatabase);
            stmt8.setFloat(1,WQAM);
            stmt8.setLong(2,AuditId);
            int affectedRows= stmt8.executeUpdate();
        } catch (Exception e) {
            System.err.println("Got an exception! ");
            System.err.println(e.getMessage());
            e.printStackTrace();


        }

    }
}