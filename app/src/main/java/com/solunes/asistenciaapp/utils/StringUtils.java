package com.solunes.asistenciaapp.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Esta clase contiene metodos para la manipulacion de strings
 */
public class StringUtils {
    private static final String TAG = "StringUtils";

    public static String DATE_FORMAT = "yyyy-MM-dd";
    public static String DATE_FORMAT_1 = "dd/MM/yyyy";
    public static String HUMAN_DATE_FORMAT = "dd, MMM yyyy";
    public static String HUMAN_HOUR_FORMAT = "HH:mm";

    /**
     * Metodo para obtener una fecha string apartir de un Date
     * @param outputFormat formato para el string
     * @param inputDate un objeto Date para procesar
     * @return retorna un string con la fecha del objeto Date
     */
    public static String formateDateFromstring(String outputFormat, Date inputDate) {
        String outputDate = "";
        SimpleDateFormat dfOutput = new SimpleDateFormat(outputFormat, java.util.Locale.getDefault());
        outputDate = dfOutput.format(inputDate);
        return outputDate;
    }

    /**
     * Metodo para aplicar un formato a un string
     * @param outputFormat formato para el string
     * @param inputDate fecha en string
     * @return retorna un objeto Date para la fecha ingresada
     */
    public static Date formateStringFromDate(String outputFormat, String inputDate) {
        SimpleDateFormat dfOutput = new SimpleDateFormat(outputFormat, java.util.Locale.getDefault());
        Date date = null;
        try {
            date = dfOutput.parse(inputDate);
            System.out.println(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     * Metodo para obtener la fecha formateado
     * @param inputDate un objeto Date para procesar
     * @return retorna una fecha en este formato: 12, Ago 2016
     */
    public static String getHumanDate(Date inputDate) {
        return formateDateFromstring(HUMAN_DATE_FORMAT, inputDate);
    }

    /**
     * Metodo para obtener una hora
     * @param inputDate un objeto Date para procesar
     * @return retorna una hora en este formato: 12:52
     */
    public static String getHumanHour(Date inputDate) {
        return formateDateFromstring(HUMAN_HOUR_FORMAT, inputDate);
    }

    /**
     * Este metodo redondea un numero double a dos digitos siempre
     * @param importe un double que es un importe
     * @return un string del double con dos decimales
     */
    public static String roundTwoDigits(double importe) {
        String res = String.valueOf(importe);
        res = res.replace(".","#");
        String[] strings = res.split("#");
        if (strings.length > 1) {
            if (strings[1].length() == 1) {
                res = res.replace("#",".");
                return res + "0";
            }
        }
        res = res.replace("#",".");
        return res;
    }
}
