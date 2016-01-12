-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------
-- -----------------------------------------------------
-- Schema instructable_kb
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema instructable_kb
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `instructable_kb` DEFAULT CHARACTER SET utf8 ;
USE `instructable_kb` ;

-- -----------------------------------------------------
-- Table `instructable_kb`.`concept_fields`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `instructable_kb`.`concept_fields` (
  `user_id` VARCHAR(45) NOT NULL,
  `concept_name` VARCHAR(45) NOT NULL,
  `field_name` VARCHAR(45) NOT NULL,
  `field_type` VARCHAR(45) NULL DEFAULT NULL,
  `isList` BIT(1) NULL DEFAULT NULL,
  `mutable` BIT(1) NULL DEFAULT NULL,
  `insert_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `concept_name`, `field_name`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `instructable_kb`.`concepts`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `instructable_kb`.`concepts` (
  `user_id` VARCHAR(45) NOT NULL,
  `concept_name` VARCHAR(45) NOT NULL DEFAULT '',
  `insert_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `concept_name`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8
COMMENT = 'The user_id can be used as a group_id (and perhaps be null, to include everyone).';


-- -----------------------------------------------------
-- Table `instructable_kb`.`email_passwords`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `instructable_kb`.`email_passwords` (
  `userName` VARCHAR(45) NOT NULL,
  `email` VARCHAR(45) NULL DEFAULT NULL,
  `enc_password` VARCHAR(45) NULL DEFAULT NULL,
  `salt` VARCHAR(96) NULL DEFAULT NULL,
  `iv` VARCHAR(96) NULL DEFAULT NULL,
  PRIMARY KEY (`userName`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `instructable_kb`.`instance_values`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `instructable_kb`.`instance_values` (
  `user_id` VARCHAR(45) NOT NULL,
  `concept_name` VARCHAR(45) NOT NULL,
  `instance_name` VARCHAR(45) NOT NULL,
  `field_name` VARCHAR(45) NOT NULL,
  `field_jsonval` VARCHAR(1000) NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`, `concept_name`, `instance_name`, `field_name`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `instructable_kb`.`instances`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `instructable_kb`.`instances` (
  `user_id` VARCHAR(45) NOT NULL DEFAULT '',
  `concept_name` VARCHAR(45) NOT NULL,
  `instance_name` VARCHAR(45) NOT NULL DEFAULT '',
  `mutable` BIT(1) NULL DEFAULT NULL,
  `insert_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `instance_name`, `concept_name`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `instructable_kb`.`interaction`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `instructable_kb`.`interaction` (
  `user_id` VARCHAR(45) NULL DEFAULT NULL,
  `full_alt` VARCHAR(3000) NULL DEFAULT NULL,
  `sentence` VARCHAR(1000) NULL DEFAULT NULL,
  `logical_form` VARCHAR(2000) NULL DEFAULT NULL,
  `reply` VARCHAR(2000) NULL DEFAULT NULL,
  `success` BIT(1) NULL DEFAULT NULL,
  `insertTime` DATETIME NULL DEFAULT CURRENT_TIMESTAMP)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `instructable_kb`.`lex_entries`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `instructable_kb`.`lex_entries` (
  `user_id` VARCHAR(45) NULL DEFAULT NULL,
  `lex_entry` VARCHAR(2000) NULL DEFAULT NULL,
  `insert_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8
COMMENT = 'user_id can also be general, for all users. No Primary Key.';


-- -----------------------------------------------------
-- Table `instructable_kb`.`parse_examples`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `instructable_kb`.`parse_examples` (
  `user_id` VARCHAR(45) NULL DEFAULT NULL,
  `example_sentence` VARCHAR(500) NULL DEFAULT NULL,
  `example_lf` VARCHAR(2000) NULL DEFAULT NULL,
  `insert_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8
COMMENT = 'user_id can be general in order to include everyone. No Primary Key.';


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
