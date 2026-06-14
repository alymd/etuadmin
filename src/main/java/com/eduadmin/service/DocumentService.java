package com.eduadmin.service;

import com.eduadmin.model.*;
import com.eduadmin.model.Module;
import com.eduadmin.repository.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final NoteRepository noteRepository;
    private final InscriptionRepository inscriptionRepository;
    private final ModuleRepository moduleRepository;
    private final EvaluationService evaluationService;

    // --- GENERATION PDF ---

    public byte[] genererReleveSemestrielPDF(Inscription inscription) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Style de polices
            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, com.lowagie.text.Font.NORMAL);
            com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, com.lowagie.text.Font.NORMAL);
            com.lowagie.text.Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10, com.lowagie.text.Font.NORMAL);
            com.lowagie.text.Font textBoldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, com.lowagie.text.Font.NORMAL);

            // En-tête de l'université
            Paragraph header = new Paragraph("UNIVERSITÉ DE DÉMONSTRATION\nSERVICES ACADÉMIQUES & DE SCOLARITÉ\n", textBoldFont);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            document.add(new Paragraph("\n"));

            // Titre du relevé
            Paragraph title = new Paragraph("RELEVÉ DE NOTES SEMESTRIEL", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Semestre : " + inscription.getSemestre().getCode() + " | Année Universitaire : " + inscription.getAnneeUniversitaire().getLibelle(), textBoldFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);

            document.add(new Paragraph("\n\n"));

            // Informations de l'étudiant
            Etudiant etudiant = inscription.getEtudiant();
            Utilisateur user = etudiant.getUtilisateur();

            PdfPTable studentTable = new PdfPTable(2);
            studentTable.setWidthPercentage(100);
            studentTable.setSpacingAfter(20);

            studentTable.addCell(createCell("Matricule : " + etudiant.getMatricule(), textFont, false));
            studentTable.addCell(createCell("Filière : " + (etudiant.getFiliereActuelle() != null ? etudiant.getFiliereActuelle().getNom() : "Non orienté"), textFont, false));
            studentTable.addCell(createCell("Nom : " + user.getNom(), textFont, false));
            studentTable.addCell(createCell("Prénom : " + user.getPrenom(), textFont, false));
            studentTable.addCell(createCell("Niveau : " + etudiant.getNiveau(), textFont, false));
            studentTable.addCell(createCell("Date de génération : " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), textFont, false));

            document.add(studentTable);

            // Table des notes
            PdfPTable table = new PdfPTable(9);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3.0f, 3.0f, 1.2f, 1.0f, 1.0f, 1.0f, 1.0f, 1.2f, 1.2f});
            table.setSpacingBefore(10);

            // En-têtes de colonnes
            String[] headers = {"Module", "Matière", "Crédits", "Coeff", "CC", "TP", "Exam", "Générale", "Décision"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }

            // Charger les modules et matières de la filière de l'étudiant
            List<Module> modules;
            if (inscription.getEtudiant().getFiliereActuelle() != null) {
                modules = moduleRepository.findBySemestreIdAndFiliereId(inscription.getSemestre().getId(), inscription.getEtudiant().getFiliereActuelle().getId());
            } else {
                modules = moduleRepository.findBySemestreId(inscription.getSemestre().getId());
            }
            for (Module m : modules) {
                boolean firstRow = true;
                List<Matiere> matieres = m.getMatieres();
                
                for (Matiere mat : matieres) {
                    // Module (fusionner si possible ou répéter)
                    if (firstRow) {
                        PdfPCell mCell = new PdfPCell(new Phrase(m.getNom(), textBoldFont));
                        mCell.setRowspan(matieres.size());
                        mCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        table.addCell(mCell);
                        firstRow = false;
                    }

                    // Matière
                    table.addCell(new Phrase(mat.getNom(), textFont));

                    // Crédit, Coeff
                    table.addCell(createCell(String.valueOf(mat.getCredits()), textFont, true));
                    table.addCell(createCell(String.valueOf(mat.getCoefficient()), textFont, true));

                    // Notes
                    Optional<Note> noteOpt = noteRepository.findByInscriptionIdAndMatiereId(inscription.getId(), mat.getId());
                    if (noteOpt.isPresent()) {
                        Note note = noteOpt.get();
                        table.addCell(createCell(note.getNoteCC() != null ? String.valueOf(note.getNoteCC()) : "-", textFont, true));
                        table.addCell(createCell(note.getNoteTP() != null ? String.valueOf(note.getNoteTP()) : "-", textFont, true));
                        table.addCell(createCell(note.getNoteExamen() != null ? String.valueOf(note.getNoteExamen()) : "-", textFont, true));
                        table.addCell(createCell(note.getNoteGenerale() != null ? String.valueOf(note.getNoteGenerale()) : "-", textBoldFont, true));
                        table.addCell(createCell(note.isValidee() ? "Validé" : "Non Val.", textFont, true));
                    } else {
                        table.addCell(createCell("-", textFont, true));
                        table.addCell(createCell("-", textFont, true));
                        table.addCell(createCell("-", textFont, true));
                        table.addCell(createCell("-", textFont, true));
                        table.addCell(createCell("N/A", textFont, true));
                    }
                }
            }

            document.add(table);

            document.add(new Paragraph("\n"));

            // Synthèse
            Paragraph synthese = new Paragraph();
            synthese.setFont(textFont);
            synthese.add(new Chunk("Moyenne Semestrielle : ", textBoldFont));
            synthese.add(new Chunk(inscription.getMoyenneSemestre() != null ? String.valueOf(inscription.getMoyenneSemestre()) + " / 20" : "N/A"));
            synthese.add(new Chunk("\nCrédits ECTS obtenus : ", textBoldFont));
            synthese.add(new Chunk(inscription.getCreditsObtenus() != null ? String.valueOf(inscription.getCreditsObtenus()) + " / 30" : "0 / 30"));
            synthese.add(new Chunk("\nDécision : ", textBoldFont));
            synthese.add(new Chunk(inscription.isValide() ? "SEMESTRE VALIDÉ" : "SEMESTRE NON VALIDÉ", textBoldFont));
            MentionAcademique mention = evaluationService.calculerMention(inscription.getMoyenneSemestre());
            synthese.add(new Chunk("\nMention : ", textBoldFont));
            synthese.add(new Chunk(mention.getLibelle(), textBoldFont));
            document.add(synthese);

            // Signature
            Paragraph signature = new Paragraph("\n\nLe Recteur / Le Chef de Scolarité\n(Signature et Cachet)", textBoldFont);
            signature.setAlignment(Element.ALIGN_RIGHT);
            document.add(signature);

            document.close();
        } catch (DocumentException e) {
            log.error("Erreur lors de la generation du PDF du releve semestriel : {}", e.getMessage());
        }

        return out.toByteArray();
    }

    private PdfPCell createCell(String text, com.lowagie.text.Font font, boolean center) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        if (center) {
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        }
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell createCell(String text, com.lowagie.text.Font font, boolean center, boolean border) {
        PdfPCell cell = createCell(text, font, center);
        if (!border) {
            cell.setBorder(Rectangle.NO_BORDER);
        }
        return cell;
    }

    // --- EXPORTS EXCEL ---

    public byte[] exporterEtudiantsExcel(List<Etudiant> etudiants) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Étudiants");

            // Style d'en-tête
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);

            // Ligne d'en-tête
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Matricule", "Nom", "Prénom", "E-mail", "Date Naissance", "Téléphone", "Niveau", "Filière"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Remplir les données
            int rowIdx = 1;
            for (Etudiant et : etudiants) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(et.getMatricule());
                row.createCell(1).setCellValue(et.getUtilisateur().getNom());
                row.createCell(2).setCellValue(et.getUtilisateur().getPrenom());
                row.createCell(3).setCellValue(et.getUtilisateur().getEmail());
                row.createCell(4).setCellValue(et.getDateNaissance().toString());
                row.createCell(5).setCellValue(et.getTelephone() != null ? et.getTelephone() : "");
                row.createCell(6).setCellValue(et.getNiveau());
                row.createCell(7).setCellValue(et.getFiliereActuelle() != null ? et.getFiliereActuelle().getCode() : "Non orienté");
            }

            // Redimensionner les colonnes
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de l'export Excel des etudiants : {}", e.getMessage());
            return new byte[0];
        }
    }

    public byte[] exporterNotesExcel(List<Note> notes) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Notes");

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            String[] columns = {"Matricule", "Étudiant", "Matière (Code)", "Matière (Nom)", "Note CC", "Note TP", "Note Examen", "Note Rattrapage", "Note Générale", "Validée"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Note n : notes) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(n.getInscription().getEtudiant().getMatricule());
                row.createCell(1).setCellValue(n.getInscription().getEtudiant().getUtilisateur().getNom() + " " + n.getInscription().getEtudiant().getUtilisateur().getPrenom());
                row.createCell(2).setCellValue(n.getMatiere().getCode());
                row.createCell(3).setCellValue(n.getMatiere().getNom());
                
                row.createCell(4).setCellValue(n.getNoteCC() != null ? n.getNoteCC() : 0.0);
                row.createCell(5).setCellValue(n.getNoteTP() != null ? n.getNoteTP() : 0.0);
                row.createCell(6).setCellValue(n.getNoteExamen() != null ? n.getNoteExamen() : 0.0);
                row.createCell(7).setCellValue(n.getNoteRattrapage() != null ? n.getNoteRattrapage() : 0.0);
                row.createCell(8).setCellValue(n.getNoteGenerale() != null ? n.getNoteGenerale() : 0.0);
                
                row.createCell(9).setCellValue(n.isValidee() ? "OUI" : "NON");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de l'export Excel des notes : {}", e.getMessage());
            return new byte[0];
        }
    }
}
