package ru.kofa.demo.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import ru.kofa.demo.enums.UnfriendlyCountry;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

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
    private final ArrayList<String> country = new ArrayList<>();
    private final ArrayList<Integer> year2022 = new ArrayList<>();
    private final ArrayList<Integer> year2023 = new ArrayList<>();
    private final ArrayList<Integer> year2024 = new ArrayList<>();

    @Override
    public Map<String, Object> analytics(String codeEas) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> measuresList = new ArrayList<>();

        setProduct(initializationProduct(codeEas));
        if (product != null) {
            clearArrays();
            initializationYearProduct();
            initializationData();

            result.put("productName", product);
            result.put("customsDutyRate", dutyRates / 1000.0 + "%");
            result.put("wtoDutyRate", dutyRatesBto / 1000.0 + "%");
            result.put("certification", certification ? "Да" : "Нет");
            result.put("governmentPurchases", purchases ? "Да" : "Нет");
            result.put("ministryOrder", order ? "Да" : "Нет");

            Map<String, Object> productionData = new HashMap<>();
            productionData.put("2022", production[0] / 1000.0 + " млн $");
            productionData.put("2023", production[1] / 1000.0 + " млн $");
            productionData.put("2024", production[2] / 1000.0 + " млн $");
            result.put("production", productionData);

            Map<String, Object> consumptionData = new HashMap<>();
            consumptionData.put("2022", consumption[0] / 1000.0 + " млн $");
            consumptionData.put("2023", consumption[1] / 1000.0 + " млн $");
            consumptionData.put("2024", consumption[2] / 1000.0 + " млн $");
            result.put("consumption", consumptionData);

            Map<String, List<String>> measureCountries = determineMeasuresByAlgorithm();
            for (Map.Entry<String, List<String>> entry : measureCountries.entrySet()) {
                Map<String, Object> measureData = new HashMap<>();
                measureData.put("type", entry.getKey());
                measureData.put("countries", entry.getValue());
                measureData.put("description", getMeasureDescription(entry.getKey()));
                measuresList.add(measureData);
            }

            List<Map<String, Object>> topSkts = calculateTopSkts();
            result.put("topSkts", topSkts);

        } else {
            Map<String, Object> errorMeasure = new HashMap<>();
            errorMeasure.put("type", "Ошибка");
            errorMeasure.put("countries", Collections.emptyList());
            errorMeasure.put("description", "Товар с кодом " + codeEas + " не найден в базе данных");
            measuresList.add(errorMeasure);
        }

        result.put("measures", measuresList);
        result.put("productCode", codeEas.substring(0, codeEas.length() - 2) + " " + codeEas.substring(codeEas.length() - 2));
        result.put("status", product != null ? "success" : "error");

        if (product != null) {
            result.put("importVolume", year2024 != null && !year2024.isEmpty() ?
                    year2024.getFirst() / 1000 + " тыс. ед." : "Нет данных");
            result.put("dynamics", calculateDynamics());
            result.put("dutyRate", dutyRates / 1000.0 + "%");

            result.put("analysisData", getAnalysisData());
        }

        return result;
    }

    private List<Map<String, Object>> calculateTopSkts() {
        List<Map<String, Object>> topSktsList = new ArrayList<>();

        if (year2024.size() <= 1) {
            return topSktsList;
        }

        List<SktsData> sktsDataList = new ArrayList<>();

        for (int i = 1; i < year2024.size(); i++) {
            String countryName = country.get(i);
            int importVolume = year2024.get(i);
            int[] weight = getWeightByName(countryName);

            if (weight != null && weight.length >= 3 && weight[2] > 0 && importVolume > 0) {
                double skts = (double) importVolume / weight[2]; // SKTS = стоимость / вес
                sktsDataList.add(new SktsData(countryName, skts, importVolume, weight[2]));
            }
        }

        sktsDataList.sort((a, b) -> Double.compare(b.skts, a.skts));

        int count = Math.min(10, sktsDataList.size());
        for (int i = 0; i < count; i++) {
            SktsData data = sktsDataList.get(i);
            Map<String, Object> countryData = new HashMap<>();
            countryData.put("country", data.countryName);
            countryData.put("skts", String.format("%.2f", data.skts));
            countryData.put("importVolume", data.importVolume / 1000 + " тыс. ед.");
            countryData.put("weight", data.weight + " кг");
            countryData.put("rank", i + 1);
            topSktsList.add(countryData);
        }

        return topSktsList;
    }

    private static class SktsData {
        String countryName;
        double skts;
        int importVolume;
        int weight;

        SktsData(String countryName, double skts, int importVolume, int weight) {
            this.countryName = countryName;
            this.skts = skts;
            this.importVolume = importVolume;
            this.weight = weight;
        }
    }

    private Map<String, List<String>> determineMeasuresByAlgorithm() {
        Map<String, List<String>> measureCountries = new HashMap<>();

        double unfriendlyShare = calculateUnfriendlyShare();
        boolean unfriendlyImportGrowing = isUnfriendlyImportGrowing();

        // 4.1
        if (unfriendlyShare >= 30.0 && unfriendlyImportGrowing) {
            System.out.println("Применяется Шаг 4.1 (доля НС >= 30%)");
            // 4.1.1
            if (production[2] >= consumption[2]) {
                // 4.1.1.1
                System.out.println("Применяется Мера 2");
                measureCountries.put("Мера 2: Введение специальных защитных мер",
                        Arrays.asList("Все страны"));
            } else {
                // 4.1.1.2
                System.out.println("Применяется Мера 6");
                measureCountries.put("Мера 6: Поддержка экспорта продукции",
                        getFriendlyCountries());
            }
        }
        // 4.2
        else {
            if (production[2] >= consumption[2]) {
                measureCountries = analyzeNonTariffMeasures();
            } else {
                measureCountries.put("Мера 6: Поддержка экспорта продукции",
                        getFriendlyCountries());
            }
        }

        if (measureCountries.isEmpty()) {
            measureCountries.put("Мера 6: Поддержка экспорта продукции",
                    getFriendlyCountries());
        }

        return measureCountries;
    }

    private boolean isUnfriendlyImportGrowing() {
        int unfriendly2023 = getUnfriendlyImportForYear(2023);
        int unfriendly2024 = getUnfriendlyImportForYear(2024);

        return unfriendly2024 >= unfriendly2023; // Не падает или растет
    }

    private Map<String, Object> getAnalysisData() {
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("unfriendlyShare", String.format("%.1f%%", calculateUnfriendlyShare()));
        analysis.put("unfriendlyImportGrowing", isUnfriendlyImportGrowing());
        analysis.put("totalImportGrowing", isTotalImportGrowing());
        analysis.put("productionVsConsumption", production[2] >= consumption[2] ? "Производство ≥ Потреблению" : "Производство < Потреблению");
        analysis.put("governmentPurchases", purchases ? "Да" : "Нет");
        analysis.put("certification", certification ? "Да" : "Нет");
        analysis.put("ministryOrder", order ? "Да" : "Нет");
        return analysis;
    }

    private Map<String, List<String>> analyzeNonTariffMeasures() {
        Map<String, List<String>> measures = new HashMap<>();

        // 1.1
        if (production[2] < consumption[2]) {
            measures.put("Мера 6: Поддержка экспорта продукции", getFriendlyCountries());
        }
        // 1.2
        else {
            // 1.2.1
            if (!purchases) {
                measures.put("Мера 4: Введение тарифной квоты", Arrays.asList("Все страны"));
            }
            // 1.2.2
            else if (certification && order) {
                measures.put("Мера 5: Техническое регулирование и сертификация",
                        Arrays.asList("Все страны"));
            } else {
                measures.put("Мера 6: Поддержка экспорта продукции", getFriendlyCountries());
            }
        }

        return measures;
    }

    private double calculateUnfriendlyShare() {
        if (year2024.isEmpty() || year2024.size() <= 1) return 0.0;

        int totalImport = year2024.getFirst(); // Первый элемент - общий импорт
        int unfriendlyImport = getUnfriendlyImportForYear(2024);

        return totalImport > 0 ? (double) unfriendlyImport / totalImport * 100 : 0.0;
    }

    private boolean isTotalImportGrowing() {
        if (year2023.isEmpty() || year2024.isEmpty()) return false;
        return year2024.getFirst() > year2023.getFirst();
    }

    private int getUnfriendlyImportForYear(int year) {
        List<Integer> yearData = getYearData(year);
        if (yearData == null || yearData.size() <= 1) return 0;

        int total = 0;
        List<String> unfriendlyCountryNames = getUnfriendlyCountryNames();

        for (int i = 1; i < yearData.size(); i++) {
            String countryName = country.get(i);
            if (isUnfriendlyCountry(countryName)) {
                int importValue = yearData.get(i);
                total += importValue;
            }
        }

        return total;
    }

    private List<Integer> getYearData(int year) {
        switch (year) {
            case 2022: return year2022;
            case 2023: return year2023;
            case 2024: return year2024;
            default: return null;
        }
    }

    private boolean isUnfriendlyCountry(String countryName) {
        return getUnfriendlyCountryNames().contains(countryName);
    }

    private List<String> getFriendlyCountries() {
        List<String> allCountries = new ArrayList<>(country);
        if (!allCountries.isEmpty()) {
            allCountries.remove(0);
        }
        allCountries.removeAll(getUnfriendlyCountryNames());
        return allCountries.isEmpty() ? Arrays.asList("Belarus", "China", "Türkiye", "Kazakhstan") : allCountries;
    }

    private List<String> getUnfriendlyCountryNames() {
        return Arrays.stream(UnfriendlyCountry.values())
                .map(UnfriendlyCountry::getName)
                .collect(Collectors.toList());
    }

    private String getMeasureDescription(String measure) {
        Map<String, String> descriptions = Map.of(
                "Мера 1: Снижение ставки ввозной таможенной пошлины", "Снижение ставки ввозной таможенной пошлины для стимулирования импорта",
                "Мера 2: Введение специальных защитных мер", "Введение специальных защитных мер для защиты внутреннего рынка",
                "Мера 3: Введение антидемпинговой пошлины", "Введение антидемпинговой пошлины против недобросовестной конкуренции",
                "Мера 4: Введение тарифной квоты", "Введение тарифной квоты для регулирования объемов импорта",
                "Мера 5: Техническое регулирование и сертификация", "Техническое регулирование и сертификация продукции",
                "Мера 6: Поддержка экспорта продукции", "Поддержка экспорта продукции российских производителей"
        );
        return descriptions.getOrDefault(measure, "Рекомендация по таможенно-тарифному регулированию");
    }

    private void clearArrays() {
        country.clear();
        year2022.clear();
        year2023.clear();
        year2024.clear();
    }

    private Map<String, String> calculateDynamics() {
        Map<String, String> dynamics = new HashMap<>();

        if (year2022.size() <= 1 || year2023.size() <= 1 || year2024.size() <= 1) {
            dynamics.put("2022-2023", "Нет данных");
            dynamics.put("2023-2024", "Нет данных");
            return dynamics;
        }

        int import2022 = year2022.getFirst();
        int import2023 = year2023.getFirst();
        int import2024 = year2024.getFirst();

        if (import2022 == 0) {
            dynamics.put("2022-2023", "Нет данных");
        } else {
            double change2022_2023 = ((double) (import2023 - import2022) / import2022) * 100;
            dynamics.put("2022-2023", String.format("%.1f%%", change2022_2023));
        }

        if (import2023 == 0) {
            dynamics.put("2023-2024", "Нет данных");
        } else {
            double change2023_2024 = ((double) (import2024 - import2023) / import2023) * 100;
            dynamics.put("2023-2024", String.format("%.1f%%", change2023_2024));
        }

        return dynamics;
    }

    private int[] getWeightByName(String name) {
        try {
            Resource resource = new ClassPathResource("data/" + product + ".xlsx");
            if (!resource.exists()) {
                System.out.println("File data/" + product + ".xlsx not found");
                return null;
            }
            try (InputStream inputStream = resource.getInputStream();
                 Workbook workbook = new XSSFWorkbook(inputStream)) {
                Sheet sheet = workbook.getSheetAt(1);
                Iterator<Row> rowIterator = sheet.rowIterator();

                if (rowIterator.hasNext()) {
                    rowIterator.next();
                }

                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    Cell nameCell = row.getCell(0);
                    if (nameCell != null && name.equals(nameCell.getStringCellValue())) {
                        int[] weight = new int[3];
                        for (int i = 0; i < 3; i++) {
                            Cell weightCell = row.getCell(i + 1);
                            if (weightCell != null) {
                                switch (weightCell.getCellType()) {
                                    case NUMERIC:
                                        weight[i] = (int) weightCell.getNumericCellValue();
                                        break;
                                    case STRING:
                                        String value = weightCell.getStringCellValue().trim();
                                        weight[i] = value.isEmpty() ? 0 : (int) Double.parseDouble(value);
                                        break;
                                    default:
                                        weight[i] = 0;
                                }
                            }
                        }
                        return weight;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error reading weight for country: " + name + ", error: " + e.getMessage());
        }
        return null;
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
            result[i - 1] = (int) (Double.parseDouble(numberPart) * 1000);
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
                Sheet sheet = workbook.getSheetAt(0);
                Iterator<Row> rowIterator = sheet.rowIterator();
                rowIterator.next();

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
                return (int) (cell.getNumericCellValue() * 1000);
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
}