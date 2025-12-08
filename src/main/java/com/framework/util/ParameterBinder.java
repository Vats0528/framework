package com.framework.util;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.*;
import java.util.*;

/**
 * Sprint 8: Utilitaire pour lier automatiquement les paramètres de requête HTTP aux objets Java
 * en utilisant la réflexion.
 * 
 * Supporte:
 * - Objets simples (Emp, Dept, etc.)
 * - Tableaux d'objets (Emp[], Dept[], etc.)
 * - Types primitifs et wrappers
 * 
 * Exemple d'utilisation:
 * - save(Emp[] e, Dept d, String title)
 * - Les paramètres peuvent être: emp[0].name, emp[1].name, dept.id, title
 */
public class ParameterBinder {

    /**
     * Lie les paramètres de la requête HTTP aux paramètres de la méthode
     * 
     * @param parameters Les paramètres de la méthode (Parameter[])
     * @param request La requête HTTP contenant les paramètres
     * @return Un tableau d'objets correspondant aux arguments de la méthode
     */
    public static Object[] bindParameters(Parameter[] parameters, HttpServletRequest request) {
        Object[] args = new Object[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();
            String paramName = param.getName();
            
            System.out.println("  Binding paramètre: " + paramName + " (type: " + paramType.getSimpleName() + ")");
            
            try {
                // Si c'est un tableau
                if (paramType.isArray()) {
                    args[i] = bindArray(paramType.getComponentType(), paramName, request);
                }
                // Si c'est un type primitif ou String
                else if (isPrimitiveOrWrapper(paramType) || paramType == String.class) {
                    args[i] = bindPrimitive(paramType, paramName, request);
                }
                // Si c'est un objet complexe
                else {
                    args[i] = bindObject(paramType, paramName, request);
                }
            } catch (Exception e) {
                System.err.println("    Erreur lors du binding de " + paramName + ": " + e.getMessage());
                args[i] = null;
            }
        }
        
        return args;
    }

    /**
     * Lie un tableau d'objets à partir des paramètres de la requête
     * Format attendu: paramName[0].attribut, paramName[1].attribut, etc.
     * 
     * @param componentType Le type des éléments du tableau
     * @param paramName Le nom du paramètre
     * @param request La requête HTTP
     * @return Un tableau d'objets
     */
    private static Object bindArray(Class<?> componentType, String paramName, HttpServletRequest request) 
            throws Exception {
        
        // Découvrir la taille du tableau en analysant les paramètres
        Map<Integer, Map<String, String>> indexedParams = new HashMap<>();
        
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            
            // Format: paramName[index].attribut
            // Exemple: emp[0].name, emp[1].salary
            if (name.startsWith(paramName + "[")) {
                try {
                    int indexStart = name.indexOf('[') + 1;
                    int indexEnd = name.indexOf(']');
                    int index = Integer.parseInt(name.substring(indexStart, indexEnd));
                    
                    String attributeName = name.substring(indexEnd + 2); // +2 pour sauter "].
                    String value = request.getParameter(name);
                    
                    indexedParams.computeIfAbsent(index, k -> new HashMap<>()).put(attributeName, value);
                    
                    System.out.println("    Array[" + index + "]." + attributeName + " = " + value);
                } catch (Exception e) {
                    System.err.println("    Erreur parsing array parameter: " + name);
                }
            }
        }
        
        // Créer le tableau avec la bonne taille
        int maxIndex = indexedParams.keySet().stream().max(Integer::compareTo).orElse(-1);
        int arraySize = maxIndex + 1;
        
        if (arraySize == 0) {
            System.out.println("    Aucun élément trouvé pour le tableau " + paramName);
            return Array.newInstance(componentType, 0);
        }
        
        Object array = Array.newInstance(componentType, arraySize);
        
        // Remplir chaque élément du tableau
        for (Map.Entry<Integer, Map<String, String>> entry : indexedParams.entrySet()) {
            int index = entry.getKey();
            Map<String, String> attributes = entry.getValue();
            
            Object element = componentType.getDeclaredConstructor().newInstance();
            populateObject(element, attributes);
            
            Array.set(array, index, element);
        }
        
        System.out.println("    Tableau créé avec " + arraySize + " élément(s)");
        return array;
    }

    /**
     * Lie un objet complexe à partir des paramètres de la requête
     * Format attendu: paramName.attribut
     * 
     * @param paramType Le type de l'objet
     * @param paramName Le nom du paramètre
     * @param request La requête HTTP
     * @return Un objet instancié et peuplé
     */
    private static Object bindObject(Class<?> paramType, String paramName, HttpServletRequest request) 
            throws Exception {
        
        Object obj = paramType.getDeclaredConstructor().newInstance();
        Map<String, String> attributes = new HashMap<>();
        
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            String value = request.getParameter(name);
            
            // Format: paramName.attribut
            // Exemple: dept.id, dept.name
            if (name.startsWith(paramName + ".")) {
                String attributeName = name.substring(paramName.length() + 1);
                attributes.put(attributeName, value);
            }
        }
        
        populateObject(obj, attributes);
        
        return obj;
    }

    /**
     * Remplit les attributs d'un objet à partir d'une map de valeurs
     * 
     * @param obj L'objet à remplir
     * @param attributes Map des attributs (nom -> valeur)
     */
    private static void populateObject(Object obj, Map<String, String> attributes) {
        Class<?> clazz = obj.getClass();
        
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String attributeName = entry.getKey();
            String value = entry.getValue();
            
            try {
                // Chercher le setter correspondant
                Field field = findField(clazz, attributeName);
                
                if (field != null) {
                    field.setAccessible(true);
                    Object convertedValue = convertValue(value, field.getType());
                    field.set(obj, convertedValue);
                } else {
                    System.err.println("      Attribut non trouvé: " + attributeName);
                }
            } catch (Exception e) {
                System.err.println("      Erreur lors de la population de " + attributeName + ": " + e.getMessage());
            }
        }
    }

    /**
     * Cherche un field dans une classe (y compris les classes parentes)
     * 
     * @param clazz La classe
     * @param fieldName Le nom du field
     * @return Le field trouvé ou null
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Lie un type primitif ou String à partir des paramètres de la requête
     * 
     * @param paramType Le type du paramètre
     * @param paramName Le nom du paramètre
     * @param request La requête HTTP
     * @return La valeur convertie
     */
    private static Object bindPrimitive(Class<?> paramType, String paramName, HttpServletRequest request) {
        String value = request.getParameter(paramName);
        
        if (value == null) {
            // Essayer avec l'attribut (pour PUT/DELETE/POST body)
            Object attr = request.getAttribute(paramName);
            if (attr != null) {
                value = attr.toString();
            }
        }
        
        System.out.println("    Primitive " + paramName + " = " + value);
        
        return convertValue(value, paramType);
    }

    /**
     * Convertit une valeur String vers le type demandé
     * 
     * @param value La valeur String
     * @param targetType Le type cible
     * @return La valeur convertie
     */
    private static Object convertValue(String value, Class<?> targetType) {
        if (value == null || value.trim().isEmpty()) {
            return getDefaultValue(targetType);
        }
        
        try {
            if (targetType == String.class) {
                return value;
            } else if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(value);
            } else if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(value);
            } else if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(value);
            } else if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(value);
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(value);
            } else if (targetType == short.class || targetType == Short.class) {
                return Short.parseShort(value);
            } else if (targetType == byte.class || targetType == Byte.class) {
                return Byte.parseByte(value);
            } else if (targetType == char.class || targetType == Character.class) {
                return value.charAt(0);
            }
        } catch (Exception e) {
            System.err.println("      Erreur de conversion: " + value + " -> " + targetType.getSimpleName());
            return getDefaultValue(targetType);
        }
        
        return null;
    }

    /**
     * Retourne la valeur par défaut pour un type
     * 
     * @param type Le type
     * @return La valeur par défaut
     */
    private static Object getDefaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == boolean.class) return false;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return '\0';
        return null;
    }

    /**
     * Vérifie si un type est primitif ou wrapper
     * 
     * @param type Le type à vérifier
     * @return true si primitif ou wrapper
     */
    private static boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() || 
               type == Integer.class || 
               type == Long.class || 
               type == Double.class || 
               type == Float.class || 
               type == Boolean.class || 
               type == Short.class || 
               type == Byte.class || 
               type == Character.class;
    }
}
