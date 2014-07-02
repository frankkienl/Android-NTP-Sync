/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.frankkie.ontp;

/**
 *
 * @author FrankkieNL
 */
public class NtpServer {

    public NtpServer(String serverName, String displayName) {
        this.serverName = serverName;
        this.displayName = displayName;
    }
    String displayName;
    String serverName;
}
