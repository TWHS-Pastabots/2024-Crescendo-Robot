// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.commands.AutoAmp;
import frc.robot.commands.AutoSpeaker;
import frc.robot.commands.BreakBeamHandoff;
import frc.robot.commands.ShootCommand;
import frc.robot.subsystems.climber.Climber;
import frc.robot.subsystems.intake.Intake;
import frc.robot.subsystems.intake.Intake.IntakeState;
import frc.robot.subsystems.launcher.Launcher;
import frc.robot.subsystems.launcher.Launcher.LauncherState;
import edu.wpi.first.wpilibj.XboxController;
import frc.robot.subsystems.swerve.Drivebase;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.auto.NamedCommands;
import com.pathplanner.lib.commands.PathPlannerAuto;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to
 * each mode, as described in the TimedRobot documentation. If you change the
 * name of this class or
 * the package after creating this project, you must also update the
 * build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {
  /**
   * This function is run when the robot is first started up and should be used
   * for any
   * initialization code.
   */

  private Drivebase drivebase;
  private Climber climber;
  private Intake intake;
  private Launcher launcher;

  private static XboxController driver;
  private static XboxController operator;

  private Command m_autoSelected;

  private BreakBeamHandoff handoffCommand;
  private ShootCommand shootCommand;
  private AutoSpeaker autoSpeaker;
  private AutoAmp autoAmp;

  private SendableChooser<Command> m_chooser;

  @Override
  public void robotInit() {
    drivebase = Drivebase.getInstance();
    launcher = Launcher.getInstance();
    intake = Intake.getInstance();
    climber = Climber.getInstance();

    driver = new XboxController(0);
    operator = new XboxController(1);
    drivebase.resetOdometry(new Pose2d(0.0, 0.0, new Rotation2d(0)));

    handoffCommand = new BreakBeamHandoff();
    shootCommand = new ShootCommand();
    autoSpeaker = new AutoSpeaker();
    autoAmp = new AutoAmp();

    NamedCommands.registerCommand("AutoAmp", autoAmp);
    NamedCommands.registerCommand("AutoSpeaker", autoSpeaker);
    NamedCommands.registerCommand("Handoff", handoffCommand);

    m_chooser = AutoBuilder.buildAutoChooser();
    m_chooser.addOption("P1 1 Piece", new PathPlannerAuto("P1 1 Piece"));
    m_chooser.addOption("P2 1 Piece", new PathPlannerAuto("P2 1 Piece"));
    m_chooser.addOption("P3 1 Piece", new PathPlannerAuto("P3 1 Piece"));
    m_chooser.addOption("BeepBoop", new PathPlannerAuto("BeepBoop"));

    SmartDashboard.putData("Auto choices", m_chooser);

    // CameraServer.startAutomaticCapture(0);
  }

  @Override
  public void robotPeriodic() {
    CommandScheduler.getInstance().run();
    drivebase.periodic();

    launcher.launcherConnections();
    intake.intakeConnections();
    climber.climberConnections();

    launcher.printConnections();
    intake.printConnections();
    climber.printConnections();

    SmartDashboard.putNumber("Flipper Current", intake.getFlipperCurrent());
    SmartDashboard.putNumber("Pivot Current", launcher.getPivotCurrent());
    SmartDashboard.putNumber("Roller Current", intake.getRollerCurrent());

    SmartDashboard.putNumber("Flipper Position", intake.getFlipperPosition());
    SmartDashboard.putNumber("Launcher Position", launcher.getPosition());

    SmartDashboard.putString("Intake State", intake.getIntakeState());
    SmartDashboard.putBoolean("Breakbeam", launcher.getBreakBeam());

    SmartDashboard.putString("Launcher State", launcher.getLaunchState().toString());
    SmartDashboard.putBoolean("Shoot Done", autoSpeaker.isFinished());

    SmartDashboard.putNumber("Translational Velocity", drivebase.getTranslationalVelocity());

  }

  @Override
  public void autonomousInit() {
    m_autoSelected = m_chooser.getSelected();

    drivebase.resetPose(new Pose2d(1.0, 2.0, new Rotation2d(0)));
    drivebase.resetOdometry(new Pose2d(1.0, 2.0, new Rotation2d(0)));

    // drivebase.resetOdometry(PathPlannerAuto.getStaringPoseFromAutoFile(m_chooser.getSelected().getName()));
    // drivebase.resetPose(PathPlannerAuto.getStaringPoseFromAutoFile(m_chooser.getSelected().getName()));

    if (m_autoSelected != null) {
      m_autoSelected.schedule();
    }
  }

  @Override
  public void autonomousPeriodic() {
    intake.updatePose();
  }

  @Override
  public void teleopInit() {
    if (m_autoSelected != null) {
      m_autoSelected.cancel();
    }
  }

  @Override
  public void teleopPeriodic() {
    intake.updatePose();

    boolean fieldRelative = true;

    /* DRIVE CONTROLS */

    double ySpeed = driver.getLeftX();
    double xSpeed = -driver.getLeftY();
    double rot = driver.getRightX();

    if (driver.getYButton()) {
      fieldRelative = !fieldRelative;
    }
    if (driver.getAButton()) {
      drivebase.lockWheels();
      drivebase.resetOdometry(PathPlannerAuto.getStaringPoseFromAutoFile(m_chooser.getSelected().getName()));
      drivebase.resetPose(PathPlannerAuto.getStaringPoseFromAutoFile(m_chooser.getSelected().getName()));
    } else {
      drivebase.drive(xSpeed, ySpeed, rot, fieldRelative);
    }

    /* INTAKE CONTROLS */

    if (operator.getRightBumper()) {
      handoffCommand.schedule();
    }

    if (operator.getPOV() == 0) {
      launcher.setLauncherState(LauncherState.SPEAKER);
    }
    if (operator.getPOV() == 90) {
      launcher.setLauncherState(LauncherState.AMP);
    }
    if (operator.getPOV() == 180) {
      launcher.setLauncherState(LauncherState.TRAP);
    }
    if (operator.getPOV() == 270) {
      launcher.setLauncherState(LauncherState.HOLD);
    }

    if (operator.getLeftBumper()) {
      intake.setIntakeState(IntakeState.STOP);
    }

    // if (operator.getXButton()) {
    // launcher.setLauncherState(LauncherState.AMP);
    // } else if (operator.getYButton()) {
    // launcher.setLauncherState(LauncherState.TRAP);
    // } else if (operator.getLeftBumper()) {
    // intake.setIntakeState(IntakePosition.STOP);
    // } else if (operator.getLeftStickButton()) {
    // intake.setIntakeState(IntakePosition.GROUND);
    // }
    //
    // if (operator.getAButton()) {
    // launcher.setLauncherState(LauncherState.HANDOFF);
    // } else if (operator.getBButton()) {
    // launcher.setLauncherState(LauncherState.SPEAKER);
    // }

    // *CLIMBER CONTROLS */

    if (driver.getRightBumper()) {
      climber.setClimbingPower();
    } else if (driver.getLeftBumper()) {
      climber.setReverseClimberPower();
    } else {
      climber.setClimberOff();
    }

    /* LAUNCHER CONTROLS */

    // if (-operator.getRightY() > 0) {
    // launcher.setPivotPower();
    // } else if (-operator.getRightY() < 0) {
    // launcher.setReversePivotPower();
    // } else {
    // launcher.setPivotOff();
    // }

    if (operator.getRightTriggerAxis() > 0) {
      shootCommand.initialize();
      shootCommand.schedule();
    } else if (operator.getLeftTriggerAxis() > 0) {
      launcher.setLauncherOff();
      launcher.setFlickOff();
      intake.setRollerOff();
      shootCommand.cancel();
      handoffCommand.cancel();
    }

  }

  @Override
  public void disabledInit() {
  }

  @Override
  public void disabledPeriodic() {
  }

  @Override
  public void testInit() {
  }

  @Override
  public void testPeriodic() {
  }

  @Override
  public void simulationInit() {
  }

  @Override
  public void simulationPeriodic() {
  }
}
