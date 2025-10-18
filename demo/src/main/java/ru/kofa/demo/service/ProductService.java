package ru.kofa.demo.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
public class ProductService implements IProductService {
    private String product;
    private int dutyRates;
    private int dutyRatesBto;
    private boolean purchases;
    private boolean certification;
    private boolean order;
    private final int[] production = new int[3];
    private final int[] consumption = new int[3];
    private final ArrayList<Double> countrySkts = new ArrayList<>();
    private final ArrayList<String> country = new ArrayList<>();
    private final ArrayList<Integer> year2022 = new ArrayList<>();
    private final ArrayList<Integer> year2023 = new ArrayList<>();
    private final ArrayList<Integer> year2024 = new ArrayList<>();

    @Override
    public Map<String, Object> analytics(String codeEas) {
        Map<String, Object> result = new HashMap<>();
        List<String> measuresList = new ArrayList<>();

        setProduct(initializationProduct(codeEas));
        if (product != null) {
            initializationYearProduct();
            initializationData();

            Set<String> arrResult = new HashSet<>();
            for (int i = 1; i < year2022.size(); i++) {
                int a = year2022.get(i);
                int b = year2023.get(i);
                int c = year2024.get(i);

                if (a < b && b < c && c / year2024.getFirst() * 100 >= 30) {
                    if (production[2] >= consumption[2]) {
                        arrResult.add("Мера 2: Введение специальных защитных мер");
                    } else {
                        arrResult.add("Мера 6: Поддержка экспорта продукции");
                    }
                } else {
                    if (dutyRates > dutyRatesBto && production[2] >= consumption[2]) {
                        arrResult.add("Мера 1: Снижение ставки ввозной таможенной пошлины");
                    } else if (dutyRates > dutyRatesBto && production[2] < consumption[2]) {
                        arrResult.add("Мера 6: Поддержка экспорта продукции");
                    } else if (dutyRates == dutyRatesBto && production[2] < consumption[2]) {
                        initializationCountrySkts();
                        int[] weight = getWeightByName(country.get(1));
                        if (weight != null) {
                            double skts = (double) year2024.get(1) / weight[2];
                            if (c > b && Collections.min(countrySkts) == skts) {
                                arrResult.add("Мера 3: Введение антидемпинговой пошлины");
                            } else if (c > b && Collections.min(countrySkts) < skts) {
                                arrResult.add("Мера 6: Поддержка экспорта продукции");
                            }
                        }
                    } else if (dutyRates == dutyRatesBto && production[2] >= consumption[2]) {
                        if (production[2] < consumption[2]) {
                            arrResult.add("Мера 6: Поддержка экспорта продукции");
                        } else if (production[2] >= consumption[2]) {
                            if (purchases) {
                                arrResult.add("Мера 6: Поддержка экспорта продукции");
                            } else {
                                arrResult.add("Мера 4: Введение тарифной квоты");
                            }
                            if (certification && order) {
                                arrResult.add("Мера 5: Техническое регулирование и сертификация");
                            } else {
                                arrResult.add("Мера 6: Поддержка экспорта продукции");
                            }
                        }
                    }
                }
            }

            measuresList.addAll(arrResult);
        } else {
            measuresList.add("Товар с кодом " + codeEas + " не найден в базе данных");
        }

        result.put("measures", measuresList);
        result.put("productCode", codeEas);
        result.put("productName", product != null ? product : "Неизвестный товар");
        result.put("status", product != null ? "success" : "error");

        if (product != null) {
            result.put("importVolume", year2024 != null && !year2024.isEmpty() ?
                    year2024.get(1) + " ед." : "Нет данных");
            result.put("dynamics", calculateDynamics());
            result.put("dutyRate", dutyRates + "%");
        }

        return result;
    }


    private String calculateDynamics() {
        if (year2022 == null || year2023 == null || year2024 == null) {
            return "Нет данных";
        }

        int prevYear = year2023.get(1);
        int currentYear = year2024.get(1);

        if (prevYear == 0) return "Нет данных";

        double change = ((double) (currentYear - prevYear) / prevYear) * 100;
        return String.format("%.1f%%", change);
    }

    private void initializationCountrySkts() {
        for (int i = 1; i < year2024.size(); i++) {
            int[] weight = getWeightByName(country.get(i));
            assert weight != null;
            countrySkts.add((double) year2024.get(i) / weight[2]);
        }
    }

    private int[] getWeightByName(String name) {
        try {
            Resource resource = new ClassPathResource("data/" + product + ".xlsx");
            if (!resource.exists()) {
                System.out.println("File data/" + product + ".xlsx not found");
            }
            try (InputStream inputStream = resource.getInputStream();
                 Workbook workbook = new XSSFWorkbook(inputStream)) {
                Sheet sheet = workbook.getSheetAt(1);
                Iterator<Row> rowIterator = sheet.rowIterator();
                rowIterator.next();
                int[] weight = new int[3];
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    Iterator<Cell> cellIterator = row.cellIterator();
                    if (cellIterator.next().getStringCellValue().equals(name)) {
                        int c = 0;
                        cellIterator.next();
                        while (cellIterator.hasNext()) {
                            weight[c] = Integer.parseInt(cellIterator.next().toString());
                        }
                    }
                }

                return weight;
            }
        } catch (Exception e) {
            System.out.println("File not found");
            return null;
        }
    }

    private void initializationData() {
        try {
            Resource resource = new ClassPathResource("data/data.xlsx");
            try (InputStream inputStream = resource.getInputStream();
                 Workbook workbook = new XSSFWorkbook(inputStream)) {

                Sheet sheet = workbook.getSheetAt(0);

                int indexCell = findProductColumn(sheet, product);
                if (indexCell == -1) {
                    System.out.println("Product " + product + " not found in data.xlsx");
                    return;
                }

                // Ставка таможни
                Row row1 = sheet.getRow(1);
                dutyRates = getNumericCellValue(row1.getCell(indexCell));

                // Ставка ВТО
                Row row2 = sheet.getRow(2);
                dutyRatesBto = getNumericCellValue(row2.getCell(indexCell));

                // Объем производства
                Row row3 = sheet.getRow(3);
                String productionText = row3.getCell(indexCell).getStringCellValue();
                parseProductionConsumption(productionText, production);

                // Объем потребления
                Row row4 = sheet.getRow(4);
                String consumptionText = row4.getCell(indexCell).getStringCellValue();
                parseProductionConsumption(consumptionText, consumption);

                // Сертификация
                Row row5 = sheet.getRow(5);
                certification = "да".equalsIgnoreCase(row5.getCell(indexCell).getStringCellValue().trim());

                // Госзакупки
                Row row6 = sheet.getRow(6);
                purchases = "да".equalsIgnoreCase(row6.getCell(indexCell).getStringCellValue().trim());

                // Приказ Минпромторга
                Row row7 = sheet.getRow(7);
                order = "да".equalsIgnoreCase(row7.getCell(indexCell).getStringCellValue().trim());

            }
        } catch (Exception e) {
            System.out.println("Error reading data.xlsx: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int findProductColumn(Sheet sheet, String product) {
        Row headerRow = sheet.getRow(0);
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null && product.equals(cell.getStringCellValue().trim())) {
                return i;
            }
        }
        return -1;
    }

    private int getNumericCellValue(Cell cell) {
        if (cell == null) return 0;
        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) (cell.getNumericCellValue() * 1000);
            case STRING:
                String value = cell.getStringCellValue().trim();
                return value.isEmpty() ? 0 : (int) (Double.parseDouble(value) * 1000);
            default:
                return 0;
        }
    }

    private void parseProductionConsumption(String text, int[] result) {
        String[] parts = text.split("\\d{4} - ");
        for (int i = 1; i < parts.length && i - 1 < result.length; i++) {
            String numberPart = parts[i].split(" ")[0].trim();
            result[i - 1] = (int) (Double.parseDouble(numberPart) * 1000); // Конвертируем в тысячи
        }
    }

    private void initializationYearProduct() {
        try {
            Resource resource = new ClassPathResource("data/" + product + ".xlsx");
            if (!resource.exists()) {
                System.out.println("File data/" + product + ".xlsx not found");
                return;
            }
            try (InputStream inputStream = resource.getInputStream();
                 Workbook workbook = new XSSFWorkbook(inputStream)) {
                Sheet sheet = workbook.getSheetAt(0); // Лист1
                Iterator<Row> rowIterator = sheet.rowIterator();
                rowIterator.next(); // Пропускаем заголовок

                country.clear();
                year2022.clear();
                year2023.clear();
                year2024.clear();

                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();

                    Cell countryCell = row.getCell(0);
                    if (countryCell == null) continue;
                    country.add(countryCell.getStringCellValue());

                    Cell cell2022 = row.getCell(1);
                    year2022.add(getNumericValueFromCell(cell2022));

                    Cell cell2023 = row.getCell(2);
                    year2023.add(getNumericValueFromCell(cell2023));

                    Cell cell2024 = row.getCell(3);
                    year2024.add(getNumericValueFromCell(cell2024));
                }
            }
        } catch (Exception e) {
            System.out.println("File " + product + " not found");
        }
    }

    private int getNumericValueFromCell(Cell cell) {
        if (cell == null) return 0;

        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) (cell.getNumericCellValue() * 1000); // Конвертируем в тысячи
            case STRING:
                String value = cell.getStringCellValue().trim();
                if (value.isEmpty()) return 0;
                try {
                    return (int) (Double.parseDouble(value) * 1000);
                } catch (NumberFormatException e) {
                    return 0;
                }
            default:
                return 0;
        }
    }

    private String initializationProduct(String codeEas) {
        try {
            Resource resource = new ClassPathResource("data/easToProduct");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] splitLine = line.split("-");
                    if (splitLine[1].equals(codeEas)) {
                        return splitLine[0];
                    }
                }
                return null;
            }
        } catch (IOException e) {
            System.out.println("File easToProduct not found");
            return null;
        }
    }

    private void setProduct(String product) {
        this.product = product;
    }

    private void setDutyRates(int dutyRates) {
        this.dutyRates = dutyRates;
    }

    private void setDutyRatesBto(int dutyRatesBto) {
        this.dutyRatesBto = dutyRatesBto;
    }
}
