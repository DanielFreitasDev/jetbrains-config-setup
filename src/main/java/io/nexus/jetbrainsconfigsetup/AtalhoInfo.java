package io.nexus.jetbrainsconfigsetup;

import lombok.Getter;

@Getter
public class AtalhoInfo {
    private String desktopName;
    private String desktopComment;
    private String desktopExec;
    private String desktopIcon;
    private String desktopWmClass;
    private String desktopFileName;

    public AtalhoInfo(IdeInfo ideInfo) {
        String ideName = ideInfo.getNome();
        String versao = ideInfo.getVersao();

        switch (ideName) {
            case "datagrip":
                this.desktopName = "DataGrip " + versao;
                this.desktopComment = "IDE for Databases and SQL";
                this.desktopExec = "datagrip";
                this.desktopIcon = "datagrip.svg";
                this.desktopWmClass = "jetbrains-datagrip";
                break;
            case "pycharm":
                this.desktopName = "PyCharm " + versao;
                this.desktopComment = "The Only Python IDE you need";
                this.desktopExec = "pycharm";
                this.desktopIcon = "pycharm.svg";
                this.desktopWmClass = "jetbrains-pycharm";
                break;
            case "rubymine":
                this.desktopName = "RubyMine " + versao;
                this.desktopComment = "The Most Intelligent Ruby and Rails IDE";
                this.desktopExec = "rubymine";
                this.desktopIcon = "rubymine.svg";
                this.desktopWmClass = "jetbrains-rubymine";
                break;
            case "intellij-idea":
                this.desktopName = "IntelliJ IDEA Ultimate " + versao;
                this.desktopComment = "Capable and Ergonomic IDE for JVM";
                this.desktopExec = "idea";
                this.desktopIcon = "idea.svg";
                this.desktopWmClass = "jetbrains-idea";
                break;
            case "webstorm":
                this.desktopName = "WebStorm " + versao;
                this.desktopComment = "The smartest JavaScript IDE";
                this.desktopExec = "webstorm";
                this.desktopIcon = "webstorm.svg";
                this.desktopWmClass = "jetbrains-webstorm";
                break;
            default:
                // Retorna nulo se não houver um modelo
                return;
        }
        this.desktopFileName = "jetbrains-" + ideName + "-" + versao + ".desktop";
    }
}