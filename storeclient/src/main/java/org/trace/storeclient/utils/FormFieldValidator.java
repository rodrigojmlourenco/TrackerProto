/*
 * Copyright (c) 2016 Rodrigo Lourenço, Miguel Costa, Paulo Ferreira, João Barreto @  INESC-ID.
 *
 * This file is part of TRACE.
 *
 * TRACE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TRACE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TRACE.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.trace.storeclient.utils;

import org.apache.commons.validator.routines.EmailValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.vt.middleware.password.AlphabeticalSequenceRule;
import edu.vt.middleware.password.CharacterCharacteristicsRule;
import edu.vt.middleware.password.DigitCharacterRule;
import edu.vt.middleware.password.LengthRule;
import edu.vt.middleware.password.LowercaseCharacterRule;
import edu.vt.middleware.password.NonAlphanumericCharacterRule;
import edu.vt.middleware.password.NumericalSequenceRule;
import edu.vt.middleware.password.Password;
import edu.vt.middleware.password.PasswordData;
import edu.vt.middleware.password.PasswordValidator;
import edu.vt.middleware.password.QwertySequenceRule;
import edu.vt.middleware.password.Rule;
import edu.vt.middleware.password.RuleResult;
import edu.vt.middleware.password.UppercaseCharacterRule;
import edu.vt.middleware.password.WhitespaceRule;


public class FormFieldValidator {

    private final static PasswordValidator PASSWORD_VALIDATOR;
    private final static EmailValidator EMAIL_VALIDATOR = EmailValidator.getInstance();

    private final static Pattern NAME_VALIDATOR  		= Pattern.compile("^[a-zA-Z çãõéáâ]{3,64}$");
    private final static Pattern USERNAME_VALIDATOR  	= Pattern.compile("^[a-zA-Z0-9_]{5,15}$");
    private final static Pattern ADDRESS_VALIDATOR		= Pattern.compile("^[a-zA-Z0-9, ºª]{5,254}$");
    private final static Pattern PHONE_VALIDATOR		= Pattern.compile("^([+]?[0-9]{1,3})?[0-9]{9,15}$");

    private final static Pattern SUBJECT_VALIDATOR  	= Pattern.compile("^[a-zA-Z0-9_]{5,25}$");

    static {
        // password must be between 8 and 16 chars long
        LengthRule lengthRule = new LengthRule(8, 25);

        // don't allow whitespace
        WhitespaceRule whitespaceRule = new WhitespaceRule();

        // control allowed characters
        CharacterCharacteristicsRule charRule = new CharacterCharacteristicsRule();
        charRule.getRules().add(new DigitCharacterRule(1)); // require at least 1 digit in passwords
        charRule.getRules().add(new NonAlphanumericCharacterRule(1)); // require at least 1 non-alphanumeric char
        charRule.getRules().add(new UppercaseCharacterRule(1)); // require at least 1 upper case char
        charRule.getRules().add(new LowercaseCharacterRule(1)); // require at least 1 lower case char
        charRule.setNumberOfCharacteristics(3); // require at least 3 of the previous rules be met

        // don't allow alphabetical sequences
        AlphabeticalSequenceRule alphaSeqRule = new AlphabeticalSequenceRule();
        NumericalSequenceRule numSeqRule = new NumericalSequenceRule(); // don't allow numerical sequences of length 3
        QwertySequenceRule qwertySeqRule = new QwertySequenceRule(); 		// don't allow qwerty sequences

        // group all rules together in a List
        List<Rule> ruleList = new ArrayList<Rule>();
        ruleList.add(lengthRule);
        ruleList.add(whitespaceRule);
        ruleList.add(charRule);
        ruleList.add(alphaSeqRule);
        ruleList.add(numSeqRule);
        ruleList.add(qwertySeqRule);

        PASSWORD_VALIDATOR = new PasswordValidator(ruleList);
    }

    public static boolean isValidSubject(String subject){
        Matcher m = SUBJECT_VALIDATOR.matcher(subject);
        return m.matches();
    }

    public static boolean isValidEmail(String email){
        return EMAIL_VALIDATOR.isValid(email);
    }

    public static boolean isValidUsername(String username){
        Matcher m = USERNAME_VALIDATOR.matcher(username);
        return m.matches();
    }

    public static boolean isValidPassword(String password){
        RuleResult result = PASSWORD_VALIDATOR.validate(new PasswordData(new Password(password)));
        return result.isValid();

    }

    public static boolean isValidPhoneNumber(String phone){
        Matcher m = PHONE_VALIDATOR.matcher(phone);
        return m.matches();
    }

    public static boolean isValidAddress(String address){
        Matcher m = ADDRESS_VALIDATOR.matcher(address);
        return m.matches();
    }

    public static boolean isValidName(String name) {
        Matcher m = NAME_VALIDATOR.matcher(name);
        return m.matches();
    }
}

