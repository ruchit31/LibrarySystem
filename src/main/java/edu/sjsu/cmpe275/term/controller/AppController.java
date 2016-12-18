package edu.sjsu.cmpe275.term.controller;

import java.util.Map;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;
import edu.sjsu.cmpe275.term.model.Book;
import edu.sjsu.cmpe275.term.model.BookStatus;
import edu.sjsu.cmpe275.term.model.BookingCart;
import edu.sjsu.cmpe275.term.model.CartItem;
import edu.sjsu.cmpe275.term.model.Librarian;
import edu.sjsu.cmpe275.term.model.Patron;
import edu.sjsu.cmpe275.term.model.Picture;
import edu.sjsu.cmpe275.term.model.Publisher;
import edu.sjsu.cmpe275.term.service.BookService;
import edu.sjsu.cmpe275.term.service.BookStatusService;
import edu.sjsu.cmpe275.term.service.BookingCartService;
import edu.sjsu.cmpe275.term.service.CartItemService;
import edu.sjsu.cmpe275.term.service.LibrarianService;
import edu.sjsu.cmpe275.term.service.PatronService;

@Controller
@RequestMapping("/")
public class AppController {

	@Autowired
	private BookService bookService;

	@Autowired
	private LibrarianService librarianService;

	@Autowired
	private PatronService patronService;

	@Autowired
	private BookStatusService bookStatusService;

	@Autowired
	private BookingCartService bookingCartService;
	
	@Autowired
	private CartItemService cartItemService;

	@Autowired
	private static MailSender activationMailSender;

	@PersistenceContext(unitName = "CMPE275TERM")
	private EntityManager entityManager;

	@Autowired
	ServletContext context;

	/**
	 * 
	 * @param context
	 */
	public void setContext(ServletContext context) {
		this.context = context;
	}

	
	public Date globalDate = null;	

	/**
	 * 
	 * @param bookStatusService
	 */
	public void setBookStatusService(BookStatusService bookStatusService) {
		this.bookStatusService = bookStatusService;
	}

	/**
	 * 
	 * @param activationMailSender
	 */
	public static void setActivationMailSender(MailSender activationMailSender) {
		AppController.activationMailSender = activationMailSender;
	}

	/**
	 * 
	 * @param bookService
	 */
	public void setBookService(BookService bookService) {
		this.bookService = bookService;
	}

	/**
	 * 
	 * @param librarianService
	 */
	public void setLibrarianService(LibrarianService librarianService) {
		this.librarianService = librarianService;
	}

	/**
	 * 
	 * @param patronService
	 */
	public void setPatronService(PatronService patronService) {
		this.patronService = patronService;
	}
	/**
	 * 
	 * @param bookingCartService
	 */
	public void setBookingCartService(BookingCartService bookingCartService) {
		this.bookingCartService = bookingCartService;
	}
	/**
	 * 
	 * @param cartItemService
	 */
	public void setCartItemService(CartItemService cartItemService) {
		this.cartItemService = cartItemService;
	}

	/**
	 * This method will send compose and send the message
	 * 
	 * @author Pratik
	 * @param to
	 * @param activationCode
	 */
	public static void sendMail(String to, int activationCode) {
		System.out.println("to: " + to + " activationCode: " + activationCode);
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(to);
		message.setSubject("Library Management System Activation Code");
		message.setText("Thank you for creating an account at Library Management System. "
				+ "\n Please activate your account using your activation code = " + activationCode
				+ "\n Please don't reply on this email.");
		System.out.println("1");
		System.out.println(activationMailSender);
		activationMailSender.send(message);
	}

	/**
	 * SEND ANY information message to user
	 * @param to
	 * @param activationCode
	 */
	public static void sendGenericMail(String to, String subject, String body) {
		
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(to);
		message.setSubject(subject);
		message.setText(body);
		activationMailSender.send(message);
	}
	
	/**
	 * GET GO TO WELCOME PAGE
	 * 
	 * @author Pratik
	 *
	 */

	@RequestMapping(value = "/welcome", method = RequestMethod.GET)
	public ModelAndView goToWelcomePage(Model model) {
		ModelAndView welcome = new ModelAndView("welcome");
		return welcome;
	}

	/**
	 * GET ADD TO CART PAGE
	 * 
	 * @author Pratik
	 *
	 */
	@RequestMapping(value = "/addToCart/{bookISBN}", method = RequestMethod.GET)
	@Transactional(propagation = Propagation.REQUIRED)
	public String addToCart(@PathVariable("bookISBN") String isbn, Model model, HttpServletRequest request){
		//Session session = entityManager.unwrap(Session.class);
		
			System.out.println("isbn: "+isbn);
			Book book = bookService.findBookByISBN(isbn);
			System.out.println("book object: "+book);
			CartItem cartItem = null;
			try{
			Query q = entityManager.createNativeQuery("SELECT * FROM cart_item where bookid ='"+ isbn +"'",
					CartItem.class);
			cartItem = (CartItem) q.getSingleResult();
			}
			catch(Exception e1){
				System.out.println("Error: "+e1);
			}
			System.out.println("cartItem: "+cartItem);
			if(cartItem != null){
				model.addAttribute("message", "Duplicate Addition to Cart");
				model.addAttribute("httpStatus","404");
				return "Error";
			}
			try{
			if (book != null) {
				cartItem = new CartItem(book, 1);
				List<CartItem> cartItems = new ArrayList<CartItem>();
				cartItems.add(cartItem);
				System.out.println("CartItems: "+cartItems);
				String email = (String) request.getSession().getAttribute("email");
				Patron patron = patronService.findPatronByEmailId(email);
				System.out.println("patron: "+patron);
				BookingCart bookingCart = patron.getBookingCart();
				bookingCart.setCartItems(cartItems);
				System.out.println("bookingCart: "+bookingCart);
				bookingCartService.updateBookingCart(bookingCart);
				cartItem.setBookCartId(bookingCart);
				cartItem = cartItemService.saveNewCartItem(cartItem);
				System.out.println("cartItem: "+cartItem);
			}
			System.out.println("3");
		}
		catch(Exception e){
			System.out.println("Error: "+e);
			model.addAttribute("httpStatus","404");
			model.addAttribute("message","Error in AddBookToCart");
			return "Error";
		}
		return "redirect:/searchBookByTitle/" + request.getSession().getAttribute("pattern");

	}

	/**
	 * Remove all items from Cart
	 * 
	 * @author Pratik
	 *
	 */
	@RequestMapping(value = "/clearCart", method = RequestMethod.GET)
	public void clearCart(Model model) {
		BookingCart bookingCart = new BookingCart();
		bookingCart.clearCart();
	}

	/**
	 * Remove a particular item from Cart
	 * 
	 * @author Pratik
	 *
	 */
	@RequestMapping(value = "/removeFromCart/{bookISBN}", method = RequestMethod.GET)
	public String removeFromCart(@PathVariable("bookISBN") String isbn, Model model, HttpServletRequest request) {
		System.out.println("isbn: "+isbn);
		Query q = entityManager.createNativeQuery("SELECT * FROM cart_item where bookid ='"+ isbn +"'",
				CartItem.class);
		CartItem cartItem = (CartItem) q.getSingleResult();
		System.out.println("cartItem: "+cartItem);
		String bookingCartId = cartItem.getBookCartId().getBookingCartId();
		System.out.println("bookingCartId: "+bookingCartId);
		BookingCart bookingCart = cartItem.getBookCartId();
		cartItemService.deleteCartItemById(cartItem.getCartItemId());
		bookingCart.removeCartItemByISBN(isbn);
		bookingCartService.updateBookingCart(bookingCart);
		return "redirect:/cartCheckout";
	}

	/**
	 * GET GO TO Activate User Page
	 * 
	 * @author Amitesh
	 *
	 */

	@RequestMapping(value = "/activationPage", method = RequestMethod.GET)
	public ModelAndView activateUser(Model model) {
		ModelAndView activation = new ModelAndView("ActivationPage");
		return activation;
	}

	@RequestMapping(value = "/activate", method = RequestMethod.POST)
	public String activateUser(@RequestParam Map<String, String> reqParams, Model model) {
		boolean bool = false;
		if (reqParams.get("email").contains("@sjsu.edu")) {
			Librarian librarian = librarianService.findLibrarianByEmailId(reqParams.get("email"));
			try {
				bool = (Integer.parseInt(reqParams.get("activate")) == librarian.getActivationCode()) ? true : false;
			} catch (NumberFormatException e) {
				System.out.println(e);
			}
			if (librarian != null && bool) {
				librarian.setStatus(true);
				librarianService.updateLibrarian(librarian);
				model.addAttribute("message", "Account created Successfully");
				return "Login";
			} else {
				return "Error";
			}
		} else {
			System.out.println("email: " + reqParams.get("email"));
			Patron patron = patronService.findPatronByEmailId(reqParams.get("email"));
			try {
				bool = (Integer.parseInt(reqParams.get("activate")) == patron.getActivationCode()) ? true : false;
			} catch (NumberFormatException e) {
				System.out.println(e);
			}
			if (patron != null && bool) {
				patron.setStatus(true);
				patronService.updatePatron(patron);
				model.addAttribute("message", "Account created Successfully");
				return "Login";
			} else {
				return "Error";
			}
		}
	}

	/**
	 * POST AUTHENTICATE USER LOGIN PAGE
	 * 
	 * @author Pratik
	 *
	 */
	@RequestMapping(value = "/home", method = RequestMethod.POST)
	public ModelAndView authenticateUser(@RequestParam Map<String, String> reqParams, Model model,
			HttpServletRequest request) {
		ModelAndView modelAndView = null;
		if (reqParams.get("email").contains("@sjsu.edu")) {
			modelAndView = new ModelAndView("LibraryHome");
			Librarian librarian = librarianService.findLibrarianByEmailId(reqParams.get("email"));
			if (librarian != null && librarian.getPassword().equals(reqParams.get("password"))
					&& librarian.isStatus() == true) {
				// librarian.setStatus(false);
				librarianService.updateLibrarian(librarian);
				request.getSession().setAttribute("loggedIn", librarian);
				request.getSession().setAttribute("email", librarian.getEmail());
				request.getSession().setAttribute("userName", librarian.getFirstName());
				System.out.println(request.getSession().getAttribute("loggedIn"));
			} else {
				modelAndView = new ModelAndView("Login");
				model.addAttribute("message", "Authentication failed, incorrect email or password!");
			}
		} else {
			System.out.println("email: " + reqParams.get("email"));
			Patron patron = patronService.findPatronByEmailId(reqParams.get("email"));

			if (patron != null && patron.getPassword().equals(reqParams.get("password")) && patron.isStatus() == true) {
				modelAndView = new ModelAndView("PatronHome");
				// patron.setStatus(false);
				patronService.updatePatron(patron);
				request.getSession().setAttribute("loggedIn", patron);
				request.getSession().setAttribute("email", patron.getEmail());
				request.getSession().setAttribute("userName", patron.getFirstName());
				request.getSession().setAttribute("pattern", "");
				request.getSession().setAttribute("loggedIn", patron);
				request.getSession().setAttribute("userName", patron.getFirstName());

			} else {
				model.addAttribute("message", "Authentication failed, incorrect email or password!");
				modelAndView = new ModelAndView("Login");
			}
		}
		modelAndView.addObject("userEmail", request.getSession().getAttribute("userEmail"));
		model.addAttribute("pattern", "");
		return modelAndView;
	}

	/**
	 * LOGOUT A LOGGED IN USER
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/logout", method = RequestMethod.GET)
	public String signout(HttpServletRequest request) {
		request.getSession().setAttribute("loggedIn", null);
		System.out.println("After logout " + request.getSession().getAttribute("loggedIn"));
		return "Login";
	}

	/**
	 * Goto ADDNEWBOOK PAGE and search book by ISBN
	 * 
	 * @author Amitesh
	 *
	 */
	@RequestMapping(value = "/newBook", method = RequestMethod.GET)
	public ModelAndView goToAddNewBookPage(Model model, HttpServletRequest request) {
		if (request.getSession().getAttribute("loggedIn") == null) {
			ModelAndView login = new ModelAndView("Login");
			return login;
		}
		ModelAndView welcome = new ModelAndView("AddNewBook");
		return welcome;
	}

	/**
	 * Goto Patron Search Book from Database PAGE and search book by ISBN
	 * 
	 * @author Amitesh
	 *
	 */
	@RequestMapping(value = "/patronSearchBook", method = RequestMethod.GET)
	public ModelAndView patronSearchBook(HttpServletRequest request) {
		if (request.getSession().getAttribute("loggedIn") == null) {
			ModelAndView login = new ModelAndView("Login");
			return login;
		}
		ModelAndView patronSearch = new ModelAndView("PatronSearchBook");
		return patronSearch;
	}

	/**
	 * Goto Patron Search Book from Database PAGE and search book by ISBN
	 * 
	 * @author Amitesh
	 *
	 */
	@RequestMapping(value = "/patronReturnBook", method = RequestMethod.GET)
	public ModelAndView patronReturnSearch(HttpServletRequest request) {
		if (request.getSession().getAttribute("loggedIn") == null) {
			ModelAndView login = new ModelAndView("Login");
			return login;
		}
		List<BookStatus> books = bookStatusService.getListOfAllIssuedBooks();
		ModelAndView patronSearch = new ModelAndView("PatronReturnBook");
		patronSearch.addObject("books",books);
		List<String> titles = new ArrayList<String>();
		List<String> isbns = new ArrayList<String>();
		for(int i=0; i<books.size(); i++){
			titles.add(books.get(i).getBook().getTitle());
			isbns.add(books.get(i).getBook().getIsbn());
		}
		patronSearch.addObject("titles",titles);
		patronSearch.addObject("isbns",isbns);
		return patronSearch;
	}

	/**
	 * Goto ADDNEWBOOK PAGE and add book manually
	 * 
	 * @author Amitesh
	 *
	 */
	@RequestMapping(value = "/newBookManually", method = RequestMethod.GET)
	public ModelAndView goToAddNewBookManualPage(Model model, HttpServletRequest request) {
		if (request.getSession().getAttribute("loggedIn") == null) {
			ModelAndView login = new ModelAndView("Login");
			return login;
		}
		ModelAndView register = new ModelAndView("AddNewBookManually");
		return register;
	}

	/**
	 * Goto Registration PAGE to add new Patron/Librarian
	 * 
	 * @author Amitesh
	 *
	 */
	@RequestMapping(value = "/registration", method = RequestMethod.GET)
	public ModelAndView registration(Model model) {
		ModelAndView register = new ModelAndView("Registration");
		return register;
	}

	/**
	 * Goto Login PAGE to to access Patron/Librarian account
	 * 
	 * @author Amitesh
	 *
	 */
	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public ModelAndView login(Model model) {
		ModelAndView login = new ModelAndView("Login");
		model.addAttribute("message", "");
		return login;
	}

	/**
	 * CREATE NEW BOOK ON CLICKING ADD BOOK IN ADDNEWBOOK PAGE
	 * 
	 * @author Pratik
	 * @param reqParams
	 * @param file
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/newBook", method = RequestMethod.POST)
	public ModelAndView createNewBook(@RequestParam Map<String, String> reqParams,
			@RequestParam(value = "file", required = false) MultipartFile file, HttpServletRequest request) {
		Book book = new Book();
		if ((reqParams.get("isbn")) != null && (reqParams.get("isbn")).isEmpty() == false)
			book.setIsbn(reqParams.get("isbn"));
		if ((reqParams.get("author")) != null && (reqParams.get("author")).isEmpty() == false)
			book.setAuthor(reqParams.get("author"));
		if ((reqParams.get("title")) != null && (reqParams.get("title")).isEmpty() == false)
			book.setTitle(reqParams.get("title"));

		Publisher publisher = new Publisher();
		if (reqParams.get("publisher") != null && (reqParams.get("publisher")).isEmpty() == false)
			publisher.setPublisher(reqParams.get("publisher"));
		DateFormat format = new SimpleDateFormat("y");
		Date date = null;
		try {
			if (reqParams.get("yearOfPublication") != null && (reqParams.get("yearOfPublication")).isEmpty() == false) {
				date = format.parse(reqParams.get("yearOfPublication").toString());
				publisher.setYearOfPublication(date);
			}
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		Picture picture = new Picture();
		System.out.println("file: " + file);
		if (file != null)
			picture.setImage(file);
		if (!picture.getImage().isEmpty()) {
			try {
				String webAppPath = context.getRealPath("/");
				File file1 = new File(
						webAppPath + "/resources/uploaded_images/" + String.valueOf(book.getIsbn()) + ".jpg");
				FileUtils.writeByteArrayToFile(file1, picture.getImage().getBytes());
				picture.setLocation(file1.getAbsolutePath());
			} catch (IOException e) {
				System.out.println("Unable to save image " + e);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			picture.setLocation("/resources/images/book.png");
		}
		System.out.println(" 1   no of copies value is " + reqParams.get("numberOfCopies"));

		try {
			if (reqParams.get("phoneNumber") != null && (reqParams.get("phoneNumber")).isEmpty() == false)
				publisher.setPhoneNumber(Integer.parseInt(reqParams.get("phoneNumber")));
			if (reqParams.get("numberOfCopies") != null && (reqParams.get("numberOfCopies")).isEmpty() == false) {
				System.out.println("2  no of copies value is " + reqParams.get("numberOfCopies"));
				book.setNumberOfCopies(Integer.parseInt(reqParams.get("numberOfCopies")));
				book.setAvailableCopies(Integer.parseInt(reqParams.get("numberOfCopies")));
			}
		} catch (Exception e) {
			System.out.println(e);
		}
		book.setCoverImage(picture);
		book.setPublisher(publisher);
		if (reqParams.get("location") != null && (reqParams.get("location")).isEmpty() == false)
			book.setLocation(reqParams.get("location"));
		if (reqParams.get("keywords").length() > 0 && reqParams.get("keywords") != null
				&& (reqParams.get("keywords")).isEmpty() == false)
			book.setKeywords(Arrays.asList(reqParams.get("keywords").split("\\s*,\\s*")));
		book = bookService.saveNewBook(book);
		ModelAndView model = new ModelAndView("LibraryHome");
		model.addObject("httpStatus", HttpStatus.CREATED);
		model.addObject("book", book);
		model.addObject("message", "Book Added Successfully");
		return model;
	}

	/**
	 * CREATE NEW BOOK ON CLICKING ADD BOOK IN ADDNEWBOOK PAGE
	 * 
	 * @author Pratik
	 * @param book
	 * @param ucBuilder
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/newBookAPI", method = RequestMethod.POST)
	public ModelAndView createNewBookAPI(@RequestParam Map<String, String> reqParams, HttpServletRequest request) {
		Book book = new Book();
		/*
		 * System.out.println("Inside createNewBookAPI"); Iterator it =
		 * reqParams.entrySet().iterator(); while (it.hasNext()) { Map.Entry
		 * pair = (Map.Entry)it.next(); System.out.println(pair.getKey() + " = "
		 * + pair.getValue()); }
		 */
		if ((reqParams.get("isbn")) != null && (reqParams.get("isbn")).isEmpty() == false)
			book.setIsbn(reqParams.get("isbn"));
		if ((reqParams.get("author")) != null && (reqParams.get("author")).isEmpty() == false)
			book.setAuthor(reqParams.get("author"));
		if ((reqParams.get("title")) != null && (reqParams.get("title")).isEmpty() == false)
			book.setTitle(reqParams.get("title"));
		Publisher publisher = new Publisher();
		if (reqParams.get("publisher") != null && (reqParams.get("publisher")).isEmpty() == false)
			publisher.setPublisher(reqParams.get("publisher"));
		book.setAvailableCopies(Integer.parseInt(reqParams.get("numberOfCopies")));
		DateFormat format = new SimpleDateFormat("y");
		Date date = null;
		try {
			if (reqParams.get("yearOfPublication") != null && (reqParams.get("yearOfPublication")).isEmpty() == false) {
				date = format.parse(reqParams.get("yearOfPublication").toString());
				publisher.setYearOfPublication(date);
			}
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		Picture picture = new Picture();
		if (reqParams.get("image_location") != null && (reqParams.get("image_location")).isEmpty() == false)
			picture.setLocation(reqParams.get("image_location"));
		try {
			if (reqParams.get("phoneNumber") != null && (reqParams.get("phoneNumber")).isEmpty() == false)
				publisher.setPhoneNumber(Integer.parseInt(reqParams.get("phoneNumber")));
			if (reqParams.get("numberOfCopies") != null && (reqParams.get("numberOfCopies")).isEmpty() == false)
				book.setNumberOfCopies(Integer.parseInt(reqParams.get("numberOfCopies")));
		} catch (Exception e) {
			System.out.println(e);
		}
		book.setCoverImage(picture);
		book.setPublisher(publisher);
		if (reqParams.get("location") != null && (reqParams.get("location")).isEmpty() == false)
			book.setLocation(reqParams.get("location"));
		if (reqParams.get("keywords").length() > 0 && reqParams.get("keywords") != null
				&& (reqParams.get("keywords")).isEmpty() == false)
			book.setKeywords(Arrays.asList(reqParams.get("keywords").split("\\s*,\\s*")));
		book = bookService.saveNewBook(book);
		ModelAndView model = new ModelAndView("LibraryHome");
		model.addObject("httpStatus", HttpStatus.CREATED);
		model.addObject("book", book);
		model.addObject("message", "Book Added Successfully");
		return model;
	}

	/**
	 * GET BOOK BY ISBN
	 * 
	 * @author Pratik
	 * @param isbn
	 * @param model
	 * @return
	 */

	@RequestMapping(value = "/book/{bookISBN}", method = RequestMethod.GET)

	public String getBookByISBN(@PathVariable("bookISBN") String isbn, Model model, HttpServletRequest request) {
		System.out.println("getBookByISBN");
		if (request.getSession().getAttribute("loggedIn") == null) {
			return "Login";
		}

		System.out.println("Isbn Value: "+isbn);
		Query q = entityManager.createNativeQuery("SELECT * FROM book where isbn ='"+ isbn +"'",
				Book.class);
		List<Book> book = q.getResultList();
		//Book book = bookService.findBookByISBN(isbn);

		System.out.println("working getBookByISBN" + book);
		//System.out.println("book " + book);
		if (book == null) {
			System.out.println("Unable to find book as book with ISBN " + isbn + " doesnot exist");
			model.addAttribute("message", "Unable to find book as book with ISBN " + isbn + " doesnot exist");
			model.addAttribute("httpStatus", HttpStatus.NOT_FOUND);
			return "Error";
		}
		model.addAttribute("books", book);
		model.addAttribute("test", "test");
		model.addAttribute("httpStatus", HttpStatus.OK);
		return "PatronHome";
	}
	
	
	@RequestMapping(value = "/searchBookByTitle/{pattern}", method = RequestMethod.GET)
	public String searchBookByTitle(@PathVariable("pattern") String pattern, Model model, HttpServletRequest request) {
		System.out.println("Hi Search book by Title: " + pattern);
		try{
			// String pattern = reqParams.get("isbn");
			if (pattern.equals(""))
				return "PatronHome";
			request.getSession().setAttribute("pattern", pattern);
			Query q = entityManager.createNativeQuery("SELECT * FROM book where title LIKE '%" + pattern + "%'",
					Book.class);
			List<Book> books = q.getResultList();
			model.addAttribute("books", books);
		}
		catch(Exception e){
			System.out.println("Error: "+e);
		}
		return "PatronHome";
	}	
	

	@RequestMapping(value = "/cartCheckout", method = RequestMethod.GET)
	public String cartCheckout(Model model, HttpServletRequest request) {
		Query q = entityManager.createNativeQuery("SELECT * FROM cart_item", CartItem.class);
		List<CartItem> bookCart = q.getResultList();
		System.out.println("books size: " + bookCart);
		model.addAttribute("books", bookCart);
		return "IssueCheckout";
	}

	@RequestMapping(value = "/book/return/{bookISBN}", method = RequestMethod.GET)
	public ModelAndView getBookReturnByISBN(@PathVariable("bookISBN") String isbn, Model model,
			HttpServletRequest request) {
		System.out.println("getBookByISBN");
		if (request.getSession().getAttribute("loggedIn") == null) {
			ModelAndView login = new ModelAndView("Login");
			return login;
		}
		ModelAndView bookFound = new ModelAndView("BookReturnFound");
		ModelAndView bookNotFound = new ModelAndView("Error");
		Book book = bookService.findBookByISBN(isbn);
		System.out.println("working getBookByISBN" + book);
		System.out.println("book " + book);
		if (book == null) {
			System.out.println("Unable to find book as book with ISBN " + isbn + " doesnot exist");
			bookNotFound.addObject("message", "Unable to find book as book with ISBN " + isbn + " doesnot exist");
			bookNotFound.addObject("httpStatus", HttpStatus.NOT_FOUND);
			return bookNotFound;
		}
		bookFound.addObject("book", book);
		bookFound.addObject("test", "test");
		bookFound.addObject("httpStatus", HttpStatus.OK);
		return bookFound;
	}

	/**
	 * GET BOOK BY ISBN
	 * 
	 * @author Pratik
	 * @param isbn
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/book/update/{bookISBN}", method = RequestMethod.GET)
	public ModelAndView getBookByISBNForUpdate(@PathVariable("bookISBN") String isbn, Model model,
			HttpServletRequest request) {
		System.out.println("inside getBookByISBN" + isbn);
		if (request.getSession().getAttribute("loggedIn") == null) {
			ModelAndView login = new ModelAndView("Login");
			return login;
		}
		ModelAndView bookFound = new ModelAndView("LibrarianUpdateBookDetail");
		ModelAndView bookNotFound = new ModelAndView("Error");
		Book book = bookService.findBookByISBN(isbn);
		System.out.println("working getBookByISBN" + book);
		System.out.println("book " + book);
		if (book == null) {
			System.out.println("Unable to find book as book with ISBN " + isbn + " doesnot exist");
			bookNotFound.addObject("message", "Unable to find book as book with ISBN " + isbn + " doesnot exist");
			bookNotFound.addObject("httpStatus", HttpStatus.NOT_FOUND);
			return bookNotFound;
		}
		bookFound.addObject("book", book);
		bookFound.addObject("test", "test");
		bookFound.addObject("httpStatus", HttpStatus.OK);
		return bookFound;
	}

	/**
	 * DELETE AN EXISTING BOOK
	 * 
	 * @author Pratik
	 * @param isbn
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/book/{bookISBN}", method = RequestMethod.DELETE)
	public ModelAndView deleteBook(@PathVariable("bookISBN") String isbn, Model model, HttpServletRequest request) {
		ModelAndView login = new ModelAndView("Login");
		if (request.getSession().getAttribute("loggedIn") == null) {
			return login;
		}
		ModelAndView deletedBook = new ModelAndView("BookDeletedSuccessfully");
		ModelAndView notDeletedBook = new ModelAndView("Error");
		System.out.println("inside deleteBook");
		Book book = bookService.findBookByISBN(isbn);
		if (book == null) {
			notDeletedBook.addObject("message", "A book with ISBN " + isbn + " doesnot exist");
			notDeletedBook.addObject("httpStatus", HttpStatus.NOT_FOUND);
			return notDeletedBook;
		}
		if (book.getNumberOfCopies() == book.getAvailableCopies()) {
			bookService.deleteBookByISBN(isbn);
			notDeletedBook.addObject("httpStatus", HttpStatus.OK);
			return deletedBook;
		} else {
			notDeletedBook.addObject("message", "Cannot be deleted as checkout by patron");
			notDeletedBook.addObject("httpStatus", HttpStatus.FORBIDDEN);
			return notDeletedBook;
		}

	}

	/**
	 * UPDATE Book ON CLICKING UPDATE IN UPDATEBOOK PAGE
	 * 
	 * @author Pratik
	 * @param book
	 * @param model
	 * @return
	 */

	@RequestMapping(value = "/book/{bookISBN}", method = RequestMethod.POST)
	public String updateBook(@PathVariable("bookISBN") String isbn, @RequestParam Map<String, String> reqMap,
			Model model, HttpServletRequest request) {
		if (request.getSession().getAttribute("loggedIn") == null) {
			return "Login";
		}
		System.out.println("IN UPDATE METHOD" + reqMap.get("author"));
		Book book1 = bookService.findBookByISBN(isbn);
		// if(book1 == null){
		// System.out.println("Unable to update as book with id
		// "+book.getIsbn()+" doesnot exist");
		// model.addAttribute("httpStatus", HttpStatus.NOT_FOUND);
		// return "BookNotFound";
		// }
		book1.setAuthor(reqMap.get("author"));
		book1.setTitle(reqMap.get("title"));
		book1.setLocation(reqMap.get("location"));
		book1.setNumberOfCopies(Integer.parseInt(reqMap.get("numberOfCopies")));
		book1.getPublisher().setPublisher(reqMap.get("publisher"));
		bookService.updateBook(book1);
		model.addAttribute("httpStatus", HttpStatus.OK);
		return "redirect:/libraryHome";
	}

	// @RequestMapping(value="/book/{bookISBN}", method = RequestMethod.POST)
	// public String updateBook(@ModelAttribute("book") Book book,
	// Model model, HttpServletRequest request) {
	// if(request.getSession().getAttribute("loggedIn") == null){
	// return "Login";
	// }
	// System.out.println("IN UPDATE METHOD");
	// Book book1 = bookService.findBookByISBN(book.getIsbn());
	// if(book1 == null){
	// System.out.println("Unable to update as book with id "+book.getIsbn()+"
	// doesnot exist");
	// model.addAttribute("httpStatus", HttpStatus.NOT_FOUND);
	// return "BookNotFound";
	// }
	// bookService.updateBook(book);
	// model.addAttribute("httpStatus", HttpStatus.OK);
	// return "BookUpdatedSuccessfully";
	// }

	/**
	 * CREATE NEW PATRON ON CLICKING CREATE PATRON IN SIGNUP PAGE
	 * 
	 * @author Pratik
	 * @param patron
	 * @param ucBuilder
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/newPatron", method = RequestMethod.POST)
	public String createNewPatron(@ModelAttribute("patron") Patron patron, UriComponentsBuilder ucBuilder,
			Model model) {
		patron = patronService.saveNewPatron(patron);
		if (patron != null) {
			model.addAttribute("httpStatus", HttpStatus.CREATED);
			return "PatronCreationSuccess";
		} else {
			model.addAttribute("httpStatus", HttpStatus.CONFLICT);
			return "Conflict";
		}
	}

	/**
	 * Goto Patron Home page
	 * 
	 * @author Amitesh
	 *
	 */
	@RequestMapping(value = "/patronHome", method = RequestMethod.GET)
	public String patronHome(Model model, HttpServletRequest request) {
		if (request.getSession().getAttribute("loggedIn") == null) {
			// ModelAndView login = new ModelAndView("Login");
			return "Login";
		}
		// ModelAndView patron = new ModelAndView("PatronHome");
		Book book = bookService.findBookByISBN("123457777");
		// model.addAttribute("author",book.getAuthor());
		System.out.println("book: " + book);
		model.addAttribute("pattern", request.getSession().getAttribute("patron"));
		return "PatronHome";
	}

	/**
	 * Goto Librarian Home PAGE to access features
	 * 
	 * @author Amitesh
	 *
	 */
	@RequestMapping(value = "/libraryHome", method = RequestMethod.GET)
	public ModelAndView libraryHome(Model model, HttpServletRequest request) {
		System.out.println("current value in sesson is " + request.getSession().getAttribute("loggedIn"));
		if (request.getSession().getAttribute("loggedIn") == null) {
			ModelAndView login = new ModelAndView("Login");
			return login;
		}
		ModelAndView librarian = new ModelAndView("LibraryHome");
		return librarian;
	}

	/**
	 * Goto Librarian Home PAGE to access features
	 * 
	 * @author Amitesh
	 *
	 */
	@RequestMapping(value = "/updateBook", method = RequestMethod.GET)
	public ModelAndView libraryUpdateBook(HttpServletRequest request) {
		System.out.println("current value in sesson is " + request.getSession().getAttribute("loggedIn"));
		if (request.getSession().getAttribute("loggedIn") == null) {
			ModelAndView login = new ModelAndView("Login");
			return login;
		}
		ModelAndView librarian = new ModelAndView("LibrarianUpdateBook");
		return librarian;
	}

	/**
	 * Goto Librarian AddBook Manually Page
	 * 
	 * @author Amitesh
	 *
	 */
	@RequestMapping(value = "/addNewBookManually", method = RequestMethod.GET)
	public ModelAndView addNewBookManually(Model model, HttpServletRequest request) {
		if (request.getSession().getAttribute("loggedIn") == null) {
			ModelAndView login = new ModelAndView("Login");
			return login;
		}
		ModelAndView librarian = new ModelAndView("AddNewBookManually");
		return librarian;
	}

	/**
	 * Goto Patron profile page to update Patron info
	 * 
	 * @author Amitesh
	 *
	 */
	@RequestMapping(value = "/patronProfile", method = RequestMethod.GET)
	public ModelAndView patronProfile(Model model, HttpServletRequest request) {
		if (request.getSession().getAttribute("loggedIn") == null) {
			ModelAndView login = new ModelAndView("Login");
			return login;
		}
		ModelAndView patronProfile = new ModelAndView("PatronProfile");
		return patronProfile;
	}

	/**
	 * Goto Patron profile page to update Patron info
	 * 
	 * @author Amitesh
	 *
	 */
	@RequestMapping(value = "/libraryProfile", method = RequestMethod.GET)
	public ModelAndView libraryProfile(Model model, HttpServletRequest request) {
		if (request.getSession().getAttribute("loggedIn") == null) {
			ModelAndView login = new ModelAndView("Login");
			return login;
		}

		ModelAndView libraryProfile = new ModelAndView("LibrarianProfile");
		return libraryProfile;
	}

	/**
	 * Goto Error page, if resource not found
	 * 
	 * @author Amitesh
	 *
	 */
	@RequestMapping(value = "/error", method = RequestMethod.GET)
	public ModelAndView error(Model model) {
		ModelAndView error = new ModelAndView("Error");
		return error;
	}

	/**
	 * GET PATRON BY ID
	 * 
	 * @author Pratik
	 * @param patronID
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/patron/{patronUniversityID}", method = RequestMethod.GET)
	public ModelAndView getPatronByID(@PathVariable("patronUniversityID") String patronUniversityID, Model model,
			HttpServletRequest request) {
		if (request.getSession().getAttribute("loggedIn") == null) {
			ModelAndView login = new ModelAndView("Login");
			return login;
		}
		ModelAndView patronFound = new ModelAndView("PatronFound");
		ModelAndView patronNotFound = new ModelAndView("PatronNotFound");
		Patron patron = patronService.findPatronByUniversityId(patronUniversityID);
		System.out.println("patron " + patron);
		if (patron == null) {
			System.out.println("Unable to find patron as patron with ID " + patron + " doesnot exist");
			model.addAttribute("httpStatus", HttpStatus.NOT_FOUND);
			return patronNotFound;
		}
		model.addAttribute("patron", patron);
		model.addAttribute("httpStatus", HttpStatus.OK);
		return patronFound;
	}

	/*	*//**
			 * CREATE NEW LIBRARIAN ON CLICKING CREATE LIBRARIAN IN SIGNUP PAGE
			 * 
			 * @author Pratik
			 * @param librarian
			 * @param ucBuilder
			 * @param model
			 * @return
			 *//*
			 * @RequestMapping(value="/newLibrarian", method =
			 * RequestMethod.POST) public String
			 * createNewLibrarian(@ModelAttribute("librarian") Librarian
			 * librarian, UriComponentsBuilder ucBuilder, Model model) { int
			 * randomCode = (int)(Math.random() * 100000);
			 * librarian.setActivationCode(randomCode); librarian =
			 * librarianService.saveNewLibrarian(librarian); if(librarian !=
			 * null){ model.addAttribute("httpStatus", HttpStatus.CREATED);
			 * return "LibrarianCreationSuccess"; }else{
			 * model.addAttribute("httpStatus", HttpStatus.CONFLICT); return
			 * "Conflict"; } }
			 */

	/**
	 * GET LIBRARIAN BY ID
	 * 
	 * @author Pratik
	 * @param librarianID
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/librarian/{librarianUniversityID}", method = RequestMethod.GET)
	public ModelAndView getLibrarianByID(@PathVariable("librarianUniversityID") String librarianUniversityID,
			Model model, HttpServletRequest request) {
		if (request.getSession().getAttribute("loggedIn") == null) {
			ModelAndView login = new ModelAndView("Login");
			return login;
		}
		ModelAndView librarianFound = new ModelAndView("LibrarianFound");
		ModelAndView librarianNotFound = new ModelAndView("LibrarianNotFound");
		Librarian librarian = librarianService.findLibrarianByUniversityId(librarianUniversityID);
		System.out.println("librarian " + librarian);
		if (librarian == null) {
			System.out.println("Unable to find patron as patron with ID " + librarian + " doesnot exist");
			model.addAttribute("httpStatus", HttpStatus.NOT_FOUND);
			return librarianNotFound;
		}
		model.addAttribute("librarian", librarian);
		model.addAttribute("httpStatus", HttpStatus.OK);
		return librarianFound;
	}

	/**
	 * Goto Patron Home page
	 * 
	 * @author Amitesh
	 *
	 */
	@RequestMapping(value = "/deleteSearch", method = RequestMethod.GET)
	public ModelAndView deleteSearch(Model model, HttpServletRequest request) {
		if (request.getSession().getAttribute("loggedIn") == null) {
			ModelAndView delete = new ModelAndView("Login");
			return delete;
		}
		ModelAndView delete = new ModelAndView("DeleteSearch");
		return delete;
	}

	/**
	 * 
	 * @param reqParams
	 * @return
	 */
	@Transactional
	@RequestMapping(value = "/newUser", method = RequestMethod.POST)
	public ModelAndView createNewUser(@RequestParam Map<String, String> reqParams) {
		System.out.println("inside createNewUser");
		ModelAndView userActivation = null;
		try {
			userActivation = new ModelAndView("ActivationPage");
			ModelAndView errorPage = new ModelAndView("Error");
			int randomCode = (int) (Math.random() * 100000);
			if (reqParams.get("email").contains("@sjsu.edu")) {
				if (librarianService.findLibrarianByUniversityId(reqParams.get("universityId")) == null) {
					Librarian librarian = new Librarian();
					librarian.setEmail(reqParams.get("email"));
					librarian.setPassword(reqParams.get("password"));
					librarian.setUniversityId(reqParams.get("universityId"));
					librarian.setFirstName(reqParams.get("firstName"));
					librarian.setLastName(reqParams.get("lastName"));
					librarian.setActivationCode(randomCode);
					System.out.println("after randomCode");
					librarian = librarianService.saveNewLibrarian(librarian);
				} else {
					errorPage.addObject("httpStatus", "ErrorLogin");
					errorPage.addObject("message", "Id already Exist");
					return errorPage;
				}
			} else {
				if (patronService.findPatronByUniversityId(reqParams.get("universityId")) == null) {
					Patron patron = new Patron();
					String id = UUID.randomUUID().toString();
					Query insertBookingCart = entityManager.createNativeQuery("Insert into Booking_cart values('"+id+"',"+0+")");
					System.out.println(insertBookingCart);
					insertBookingCart.executeUpdate();
					BookingCart bookingCart = bookingCartService.findBookingCartById(id);
					System.out.println("bookingCart: "+bookingCart);
					patron.setEmail(reqParams.get("email"));
					patron.setPassword(reqParams.get("password"));
					patron.setUniversityId(reqParams.get("universityId"));
					patron.setFirstName(reqParams.get("firstName"));
					patron.setLastName(reqParams.get("lastName"));
					patron.setBookingCart(bookingCart);
					patron.setActivationCode(randomCode);
					patron = patronService.saveNewPatron(patron);
				} else {
					errorPage.addObject("httpStatus", "ErrorLogin");
					errorPage.addObject("message", "Id already Exist");
					return errorPage;
				}
			}
			System.out.println("Email: " + reqParams.get("email") + "randomCode: " + randomCode);
			sendMail(reqParams.get("email"), randomCode);
			userActivation.addObject("universityId", reqParams.get("universityId"));
			userActivation.addObject("email", reqParams.get("email"));
		} catch (DataIntegrityViolationException e1) {
			System.out.println("Exception: " + e1);
			userActivation = new ModelAndView("Error");
		} catch (Exception e) {
			System.out.println("Exception: " + e);
			userActivation = new ModelAndView("Error");
		}
		return userActivation;
	}

	/*	*//**
			 * 
			 * @param reqParams
			 * @param ucBuilder
			 * @param model
			 * @return
			 *//*
			 * @RequestMapping(value="/completeRegistration", method =
			 * RequestMethod.POST) public String
			 * completeUserRegistration(@RequestParam Map<String, String>
			 * reqParams, UriComponentsBuilder ucBuilder, Model model) { String
			 * email = reqParams.get("email"); String universityId =
			 * reqParams.get("universityId"); if(email.contains("@sjsu.edu")){
			 * Librarian librarian =
			 * librarianService.findLibrarianByUniversityId(universityId);
			 * if(librarian.getActivationCode() ==
			 * Integer.parseInt(reqParams.get("activationCode"))){
			 * librarian.setStatus(true);
			 * librarianService.updateLibrarian(librarian);
			 * model.addAttribute("httpStatus", HttpStatus.OK); return
			 * "userCreationSuccess"; } else{ return "wrongActivationCode"; } }
			 * else{ Patron patron =
			 * patronService.findPatronByUniversityId(universityId);
			 * if(patron.getActivationCode() ==
			 * Integer.parseInt(reqParams.get("activationCode"))){
			 * patron.setStatus(true); patronService.updatePatron(patron);
			 * model.addAttribute("httpStatus", HttpStatus.CREATED); return
			 * "userCreationSuccess"; } else{ return "wrongActivationCode"; } }
			 * }
			 */

	/**
	 * Search Books Ruchit code strts here
	 * 
	 * @param librarianID
	 * @param model
	 * @return
	 */
	
	  @RequestMapping(value = "/checkout", method = RequestMethod.GET)
	  @Transactional
	  public ModelAndView checkout(Model model,HttpServletRequest request) {
	    ModelAndView success = new ModelAndView("PatronHome");
	    ModelAndView error = new ModelAndView("Error");
	    String email = (String)request.getSession().getAttribute("email");
	    System.out.println("email: "+email);
	    Query q = entityManager.createNativeQuery("select cart_item.bookid from cart_item where bookingcartid =(select bookingcartid from patron where email='"+email+"')");
		List<String> bookList = q.getResultList();
		System.out.println("book size: "+bookList.toString());
		//Book book = bookService.findBookByISBN(isbn);
		String[] isbnArray = new String[bookList.size()];
		for(int i=0; i<isbnArray.length; i++ ){
			System.out.println("bookList "+i+" : "+bookList.get(i));
			isbnArray[i] = bookList.get(i);
			//System.out.println(isbnArray[i]);
		}
		
	    System.out.println(isbnArray[0]);
	    // String email = "kadakiaruchit@gmail.com";
	    //String email = ((Patron)request.getSession().getAttribute("loggedIn")).getEmail();
	    System.out.println(email);
	    Calendar c = new GregorianCalendar();
	    Date issueDate = c.getTime();
	    c.add(Calendar.DATE, 30);
	    Date dueDate = c.getTime();
	    Patron patron = patronService.findPatronByEmailId(email);
	    String checkoutData = "";
	    if (isbnArray.length > 5) {
	      error.addObject("message", "You cant checkout more than 5 books at a tiime");
	      return error;
	    }
	    if ((patron.getDayIssuedCount() + isbnArray.length) > 5) {
	      error.addObject("message", "You can not checkout more than 5 books in one day");
	      return error;
	    }
	    if ((patron.getTotalIssuedCount() + isbnArray.length) > 10) {
	      error.addObject("message", "You can not checkout more than total 10 books ");
	      return error;
	    }
	    System.out.println("before for in checkout ");
	    List<BookStatus> patronsBookStatus = patron.getBookStatus();
	    System.out.println("before for in checkout 1 " + patronsBookStatus.size());
	    for (int i = 0; i < isbnArray.length; i++) {
	      for (int j = 0; j < patronsBookStatus.size(); j++) {
	        if (isbnArray[i].equals(patronsBookStatus.get(j).getBook().getIsbn())&&patronsBookStatus.get(j).getRequestStatus().equals("issued")) {
	          error.addObject("message", "Book is already issued to you");
	          return error;
	        }
	        
	        if (isbnArray[i].equals(patronsBookStatus.get(j).getBook().getIsbn())&&patronsBookStatus.get(j).getRequestStatus().equals("requested")) {
	        		System.out.println("inside requested its working");
	        		bookStatusService.returnBooks(patronsBookStatus.get(j).getBookStatusId());
		        }    
	      }
	    }
	    
	   //select bookstatus.getPatron() from book_status where status="emailed" and bookid=isbn;
	   List< BookStatus> ans=null;
	 outer:   for(int i=0;i<isbnArray.length;i++){
	     Query ans1 = entityManager.createNativeQuery("SELECT * FROM book_status where requeststatus='emailed' and bookid='"+isbnArray[i]+"' ;",BookStatus.class);
	    ans=ans1.getResultList();
	    for(int j=0;j<ans.size();j++){
	    	System.out.println("in request queue manget"+ans.get(j).getPatrons().get(0).getEmail());
	    	if(!ans.get(j).getPatrons().get(0).getEmail().equals(email)){
	    		System.out.println("choorrrr saale");
	    		error.addObject("message", "Book is already on hold for another user");
	    		return error;
	    		
	    	}
	    }
	    
	    }
	    
	    System.out.println("before for in checkout ");
	    List<BookStatus> patronsBookStatus1 = patron.getBookStatus();
	    System.out.println("before for in checkout 1 " + patronsBookStatus.size());
	    
//	    for (int i = 0; i < isbnArray.length; i++) {
//	      for (int j = 0; j < patronsBookStatus.size(); j++) {
//	        if (isbnArray[i].equals(patronsBookStatus.get(j).getBook().getIsbn())&&patronsBookStatus.get(j).getRequestStatus().equals("emailed")) {
//	          error.addObject("message", "Book is already issued to you");
//	          return error;
//	        }
//	      }
//	    }
	    
	    
	    for (int i = 0; i < isbnArray.length; i++) {
	      BookStatus bookStatus = new BookStatus();
	      Book book = bookService.findBookByISBN(isbnArray[i]);
	      System.out.println("challa 1" + patron + book.getIsbn());
	      if (book.getAvailableCopies() <= 0) {
	        error.addObject("message", "Sorry, Requested book is out of stock");
	        return error;
	      }
	   
	      patron.setDayIssuedCount(patron.getDayIssuedCount() + 1);
	      patron.setTotalIssuedCount(patron.getTotalIssuedCount() + 1);
	      System.out.println(book.getIsbn() + " book bhai wala is " + book);
	      bookStatus.setDueDate(dueDate);
	      bookStatus.setIssueDate(issueDate);
	      bookStatus.setBook(book);
	      bookStatus.setRequestStatus("issued");
	      bookStatus.getPatrons().add(patron);
	      book.setAvailableCopies(book.getAvailableCopies() - 1);
	      entityManager.persist(book);
	      entityManager.persist(patron);
	      entityManager.persist(bookStatus);
	      checkoutData += "\n  ISBN: " + book.getIsbn() + " TITLE:" + book.getTitle() + "";
	    }
	    System.out.println("Hi You have just checked out following items");
	    SimpleMailMessage message = new SimpleMailMessage();
	    message.setTo(email);
	    message.setSubject("SJSU Library Checkout on " + c.getTime());
	    message.setText("Hi You have just checked out following items " + checkoutData + "\n = issueDate : " + issueDate
	    + "   DueDate : " + dueDate + "   " + "\n Please don't reply on this email.");
	    System.out.println("1");
	    System.out.println(activationMailSender);
	    activationMailSender.send(message);
	    return success;
	  }
	  
	  @RequestMapping(value = "/return", method = RequestMethod.POST)
      public ModelAndView BookReturn(@RequestParam(value = "isbn") String isbn, Model model, HttpServletRequest request) {
    	  String[] isbnArray = new String[1];
    	  isbnArray[0] = isbn;
    	  return Return(isbnArray, model, request);
      }
	
	/*
	 * Search Books Ruchit code strts here
	 * 
	 * @param librarianID
	 * 
	 * @param model
	 * 
	 * @return
	 * 
	 */
	
	///////////////Ruchit return Book code Starts ///////////////////////
	
	public ModelAndView Return(String[] isbnArray, Model model, HttpServletRequest request) {

	ModelAndView success = new ModelAndView("PatronHome");

	ModelAndView error = new ModelAndView("Error");


	System.out.println("inside checkout ");

	String email = "kadakiaruchit@gmail.com";


	// System.out.println(reqParams.get("isbn"));

	// String email =

	// ((Patron)request.getSession().getAttribute("loggedIn")).getEmail();

	System.out.println(email);

	Calendar c = new GregorianCalendar();

	Date returnDate = c.getTime();

	// int days = Days.daysBetween(returnDate, returnDate).getDays();

	// c.add(Calendar.DATE, 30);

	// Date dueDate=c.getTime();

	// System.out.println();


	String checkoutReturnData = "";


	Patron patron = patronService.findPatronByEmailId(email);


	if (isbnArray.length > 10) {


	// model.addAttribute("message2","You cant checkout more than 5

	// books at a time");

	error.addObject("message", "You cant return more than 10 books at a tiime");


	return error;


	}


	Date returndate = null;

	List<BookStatus> patronsBookStatus = patron.getBookStatus();

	for (int i = 0; i < patronsBookStatus.size(); i++) {

	System.out.println("bhaijaan" + patronsBookStatus.get(i).getBookStatusId() + "  "

	+ patronsBookStatus.get(i).getBook().getIsbn());

	for (int j = 0; j < isbnArray.length; j++) {


	if (isbnArray[j].equals(patronsBookStatus.get(i).getBook().getIsbn())) {

	System.out.println("deleting book isbn of " + isbnArray[j]);

	checkoutReturnData += "\n" + patronsBookStatus.get(i).getBook().getIsbn() + "  "

	+ patronsBookStatus.get(i).getBook().getTitle() + "  "

	+ patronsBookStatus.get(i).getIssueDate();

	System.out.println("penalty deleting book isbn of " + isbnArray[j]);

	returndate = (Date) request.getSession().getAttribute("appTime");

	long penalty =  (returndate.getTime() - patronsBookStatus.get(i).getDueDate().getTime())/ (1000 * 60 * 60 * 24);

	System.out.println("aaj ka date is " + returndate);

	System.out.println("due ka date is " + patronsBookStatus.get(i).getDueDate());

	Book b=patronsBookStatus.get(i).getBook();

	System.out.println("book is"+b+" "+b.getAvailableCopies());

	b.setAvailableCopies(b.getAvailableCopies()+1);

	bookService.updateBook(b);

	System.out.println(penalty);

	patron.setTotalIssuedCount(patron.getTotalIssuedCount() - 1);


	if (penalty > 0) {


	System.out.println("high heels ");

	patron.setPenalty(patron.getPenalty() + (int)penalty);



	}

	patronService.updatePatron(patron);

	bookStatusService.returnBooks(patronsBookStatus.get(i).getBookStatusId());
	break;

	}


	}


	}


	System.out.println("Hi You have just Returned out following item");

	SimpleMailMessage message = new SimpleMailMessage();

	message.setTo(email);

	message.setSubject("SJSU Library Return on " + c.getTime());

	message.setText("Hi You have just return out following items " + checkoutReturnData + "     Rerturn date is "

	+ returndate + "\n = " + c.getTime() + "\n Please don't reply on this email.");

	System.out.println("bhaijaan mail bje dia");

	System.out.println(activationMailSender);

	activationMailSender.send(message);
	checkFunctionalityAtReturn(isbnArray);
	return success;
	

	}
	//////////////Ruchit return Book code Ends here /////////////////////
	  
	  
	  
	  /**
	 * Will set date and time of application as input by user in variable
	 * "appTIme"
	 * 
	 * @param reqParams
	 * @param request
	 */
	@RequestMapping(value = "/setDateTime", method = RequestMethod.POST)
	@Transactional
	public void setDateTime(@RequestParam Map<String, String> reqParams, HttpServletRequest request) {
		System.out.println("Setting time");
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		Date date = null;
		try {
			date = formatter.parse(reqParams.get("appTime"));
			globalDate = date;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		request.getSession().setAttribute("appTime", date);
		// Will Execute code for sending reminders
		sendDueReminder();
		removeRequestAfterThreeDays();
	}

	@RequestMapping(value = "/requestBook/{bookISBN}", method = RequestMethod.POST)
	@Transactional
	public ModelAndView set(@PathVariable("bookISBN") String isbn, HttpServletRequest request) {
		ModelAndView requestSuccess = new ModelAndView("BookRequestSuccess");
		Book book = bookService.findBookByISBN(isbn);
		if (book.getAvailableCopies() == 0) {
			String email = (String) request.getSession().getAttribute("email");
			System.out.println("email address is " + email);
			Patron patron = patronService.findPatronByEmailId(email);
			BookStatus bookstatus = new BookStatus();
			System.out.println("date to be set is " + (Date) request.getSession().getAttribute("appTime"));
			bookstatus.setAssignedDate((Date) request.getSession().getAttribute("appTime"));
			bookstatus.setRequestDate((Date) request.getSession().getAttribute("appTime"));
			bookstatus.setRequestStatus("requested");
			bookstatus.getPatrons().add(patron);
			bookstatus.setBook(book);
			// patron.getBookStatus().add(bookstatus);
			// patronService.updatePatron(patron);
			// entityManager.persist(book);
			entityManager.persist(patron);
			entityManager.persist(bookstatus);
			requestSuccess.addObject("message", "book have been requested");
			return requestSuccess;
		} else {
			ModelAndView error = new ModelAndView("BookRequestError");
			error.addObject("message", "Book is available");
			return error;
		}
	}

	@RequestMapping("/tester")
	public void tester(HttpServletRequest request) {
		String email = (String) request.getSession().getAttribute("email");
		System.out.println(email);
	}


	
	public List<BookStatus> selectEmailed(){
		List<BookStatus> bookstatuslist = null;
		Query selectEmailedRecodrs = entityManager.createNativeQuery("SELECT * FROM cmpe275termdb.book_status where requeststatus = 'emailed';", BookStatus.class);
		bookstatuslist = selectEmailedRecodrs.getResultList();
		return bookstatuslist;
	}
	
/*	public void selectRequested(String isbn){
		List<BookStatus> requestedBookstatuslist = null;
		Query selectRequestedRecodrs = entityManager.createNativeQuery("SELECT * FROM cmpe275termdb.book_status where requeststatus = 'requested' and bookid = '" + isbn + "';", BookStatus.class);
		requestedBookstatuslist = selectRequestedRecodrs.getResultList();
		int i = 0;
		List<String> isbnList = new ArrayList<String>();
		while(requestedBookstatuslist.size() > i){
			isbnList.add(requestedBookstatuslist.get(i).getBook().get);
		}
	}*/
	
	
	public void deleteRowpatron_bookstatus(String bookStatusId){
		entityManager.getTransaction().begin();
		Query removeFromPatron_bookstatus = entityManager.createNativeQuery("DELETE FROM cmpe275termdb.patron_bookstatus WHERE book_status_id='" + bookStatusId + "'; ");
		System.out.println(removeFromPatron_bookstatus);
		removeFromPatron_bookstatus.executeUpdate();
		entityManager.getTransaction().commit();
		//removeFromPatron_bookstatus.getResultList();
	}
	
	
	public void deleteRowbook_status(String bookStatusId){
		entityManager.getTransaction().begin();
		Query removeFromBook_status = entityManager.createNativeQuery("DELETE FROM cmpe275termdb.book_status WHERE bookstatusid='" + bookStatusId + "';");
		System.out.println(removeFromBook_status);
		removeFromBook_status.executeUpdate();
		entityManager.getTransaction().commit();
		//removeFromBook_status.getResultList();
	}
	
	//@Scheduled(fixedRate = 1000 * 10)
	public void removeRequestAfterThreeDays(){
		List<BookStatus> bookstatuslist = selectEmailed();
		System.out.println("size of fetched result is " + bookstatuslist.size());
		Date todayDate = globalDate;
		System.out.println("printing todays date " + todayDate);
		int i = 0;
		List<String> isbns = new ArrayList<String>();
		while(bookstatuslist.size() > i){
			Date assignedDate = bookstatuslist.get(i).getAssignedDate();
			long x = (todayDate.getTime()-assignedDate.getTime());
			long passedDays = x/(1000 * 60 * 60 * 24);
			int count = (int)passedDays;
			System.out.println("NUmber of days " + count);
			if(count > 3){
				System.out.println("inside Loop");
				//Function to find all other requests for the same book
				//selectRequested(bookstatuslist.get(i).getBook().getIsbn());
				isbns.add(bookstatuslist.get(i).getBook().getIsbn());
				String bookStatusId = bookstatuslist.get(i).getBookStatusId();
				bookStatusService.returnBooks(bookStatusId);
/*				//Query q = entityManager.createNativeQuery("SELECT email FROM cmpe275termdb.patron_bookstatus where book_status_id = '" + bookStatusId + "';");
				//List<String> strList = q.getResultList();
				//Patron patron = patronService.findPatronByEmailId(strList.get(0));
				//System.out.println("first Name is " + patron.getFirstName());
				System.out.println("calling patron_bookstatus");
				deleteRowpatron_bookstatus(bookStatusId);
				System.out.println("calling book_status");
				deleteRowbook_status(bookStatusId);
				//bookstatuslist.get(i).getPatrons().remove(patron);
				//entityManager.persist(bookstatuslist.get(i));
*/			}
			i++;
		}
		String[] arr = new String[isbns.size()];
		for(int j = 0; j < isbns.size(); j++){
			arr[j] = isbns.get(j);
		}
		checkFunctionalityAtReturn(arr);
		System.out.println("cron job running");

	}
	
	public List<BookStatus> findBookStatusForISBN(String isbn){
		List<BookStatus> bookstatuslist = null;
		Query selectListofBookStaus = entityManager.createNativeQuery("SELECT * FROM cmpe275termdb.book_status where bookid = '" + isbn + "';", BookStatus.class);
		bookstatuslist = selectListofBookStaus.getResultList();
		return bookstatuslist;
	}


	public void checkFunctionalityAtReturn(String[] isbnArray) {
		System.out.println("Inside CheckFuncationalityAt Return");
		Date minDate = new Date(Long.MAX_VALUE);
		BookStatus bookStatus1 = null;
		for (int j = 0; j < isbnArray.length; j++) {
			List<BookStatus> bookStatus = findBookStatusForISBN(isbnArray[j]);
			if(bookStatus != null){
				System.out.println("Total bookstatuses for  " + isbnArray[j] + " is " + bookStatus.size());
				int k = 0;
				while(bookStatus.size() > k){
					if (bookStatus.get(k).getRequestStatus().equals("requested")) {
						if (minDate.compareTo(bookStatus.get(k).getRequestDate()) > 0) {
							minDate = bookStatus.get(k).getRequestDate();
							bookStatus1 = bookStatus.get(k);
						}
						System.out.println("BookStatus Id for min value" + bookStatus1.getBookStatusId());
						String email = getPatronByBookStatusId(bookStatus1.getBookStatusId());
						System.out.println("Sending email to user");
						SimpleMailMessage message = new SimpleMailMessage();
						message.setTo(email);
						message.setSubject(
								"Your requested Book is now available");
						message.setText(
								"Thank you for requesting the book. \n The Book you requested is now available. Kindly issue it within 3 days. "
										+ "Otherwise your request would be neglected. \n It is an auto generated email. Please don't reply on this email.");
						activationMailSender.send(message);
						System.out.println("Setting request status to emailed");
						bookStatus1.setRequestStatus("emailed");
						bookStatusService.updateBookStatus(bookStatus1);
					}
					k++;
				}
			}
		}
	}
	
	public void sendDueReminder(){
		List<Patron> allPatron = patronService.findAllPatron();
		int i = 0;
		System.out.println("Total patrons fetched " + allPatron.size());
		while(allPatron.size() > i){
			System.out.println("Mail of he Patron" + allPatron.get(i).getEmail());
			List<BookStatus> bookStatusList = allPatron.get(i).getBookStatus();
			int j = 0;
			String body = "";
			System.out.println("Total Bookstatus fetched for a Patron" + bookStatusList.size());
			while(bookStatusList.size() > j){
				Date dueDate = bookStatusList.get(j).getDueDate();
				System.out.println("DueDate " + dueDate);
				Date todayDate = globalDate;
				System.out.println("GLOBAL date " + globalDate);
				if(dueDate != null && todayDate != null){
					long x = (dueDate.getTime() - todayDate.getTime());
					long daysLeft = x/(1000 * 60 * 60 * 24);
					int count = (int)daysLeft;
					System.out.println("Count is " + count);
					// change Request status to issued while issuing 
					if(bookStatusList.get(j).getRequestStatus().equals("issued") && count <= 5 && count > 0){
						String isbn = bookStatusList.get(j).getBook().getIsbn();
						String bookName = bookStatusList.get(j).getBook().getTitle();
						body = body + "Book " + bookName + " " + " ISBN: " +  isbn + " is due on " + dueDate + "\n";
					}
				}
				j++;
			}
			if(!body.equals("")){
				System.out.println(body);
				String email = allPatron.get(i).getEmail();
				String subject = "Book Due Date Reminder";
				sendGenericMail(email, subject, body);
			}
			i++;
		}

	}

	public String getPatronByBookStatusId(String bookStatusId){
		Query getPatronByBookStatusId = entityManager.createNativeQuery("Select email FROM cmpe275termdb.patron_bookstatus WHERE book_status_id='" + bookStatusId + "';");
		System.out.println(getPatronByBookStatusId);
		String email = (String) getPatronByBookStatusId.getSingleResult();
		return email;
	}
}

// patron cant keep a more than 1 boook for same isbn